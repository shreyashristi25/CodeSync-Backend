package com.codesync.collab.service;

import com.codesync.collab.dto.CollabHistoryResponse;
import com.codesync.collab.dto.CollabHistoryResponse.HistoryParticipantView;
import com.codesync.collab.dto.CollabBroadcastEnvelope;
import com.codesync.collab.dto.CollabMessageType;
import com.codesync.collab.dto.CollabSessionResponse;
import com.codesync.collab.dto.AdminSessionView;
import com.codesync.collab.dto.JoinCollabRequest;
import com.codesync.collab.exception.BadRequestException;
import com.codesync.collab.exception.NotFoundException;
import com.codesync.collab.model.CollabHistory;
import com.codesync.collab.model.CollabSession;
import com.codesync.collab.model.Participant;
import com.codesync.collab.redis.CollabRedisPublisher;
import com.codesync.collab.repository.CollabHistoryRepository;
import com.codesync.collab.repository.CollabSessionRepository;
import com.codesync.collab.repository.ParticipantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollabService {
    private final CollabSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final CollabHistoryRepository historyRepository;
    private final CollabRedisPublisher collabRedisPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public CollabSessionResponse join(Long fileId, JoinCollabRequest request) {
        CollabSession session = sessionRepository
                .findByFileId(fileId)
                .orElseGet(() -> {
                    String uuid = java.util.UUID.randomUUID().toString();
                    return sessionRepository.save(CollabSession.builder()
                            .sessionUuid(uuid)
                            .fileId(fileId)
                            .hostUserId(request.getUserId())
                            .createdAt(Instant.now())
                            .lastActivityAt(Instant.now())
                            .participants(new ArrayList<>())
                            .build());
                });
        
        if (session.getSessionPassword() != null && !session.getSessionPassword().isEmpty()) {
            if (request.getSessionPassword() == null || !request.getSessionPassword().equals(session.getSessionPassword())) {
                throw new BadRequestException("Session password required");
            }
        }
        
        if (session.getMaxParticipants() != null && session.getParticipants().size() >= session.getMaxParticipants()) {
            throw new BadRequestException("Session is full");
        }
        
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);
        
        if (participantRepository.findBySession_IdAndUserId(session.getId(), request.getUserId()).isPresent()) {
            return toResponse(fileId, session.getId(), session.getSessionUuid(), session.getHostUserId());
        }
        Participant participant = Participant.builder()
                .session(session)
                .userId(request.getUserId())
                .displayName(request.getDisplayName())
                .joinedAt(Instant.now())
                .build();
        participantRepository.save(participant);
        Map<String, Object> payload = new HashMap<>();
        payload.put("displayName", request.getDisplayName());
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.JOIN)
                .userId(request.getUserId())
                .payload(payload)
                .build());
        return toResponse(fileId, session.getId(), session.getSessionUuid(), session.getHostUserId());
    }

    @Transactional
    public CollabSessionResponse startSession(Long fileId, JoinCollabRequest request) {
        // Always delegate to join() which is now fully idempotent
        return join(fileId, request);
    }

    @Transactional
    public void leave(Long fileId, String userId) {
        CollabSession session =
                sessionRepository.findByFileId(fileId).orElseThrow(() -> new NotFoundException("Session not found"));
        Participant leaving = participantRepository.findBySession_IdAndUserId(session.getId(), userId)
                .orElseThrow(() -> new NotFoundException("Participant not in session"));
        String displayName = leaving.getDisplayName();
        participantRepository.deleteBySession_IdAndUserId(session.getId(), userId);
        
        // Host auto-transfer: if the leaving user is the host, pick a new host
        if (userId.equals(session.getHostUserId())) {
            List<Participant> remaining = participantRepository.findAllBySession_Id(session.getId());
            if (!remaining.isEmpty()) {
                // Sort by joinedAt ascending (earliest joiner becomes new host)
                remaining.sort((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()));
                String newHostId = remaining.get(0).getUserId();
                String newHostName = remaining.get(0).getDisplayName();
                session.setHostUserId(newHostId);
                sessionRepository.save(session);
                
                // Broadcast host transfer via a JOIN message with transfer info
                Map<String, Object> transferPayload = new HashMap<>();
                transferPayload.put("newHostUserId", newHostId);
                transferPayload.put("newHostDisplayName", newHostName);
                transferPayload.put("hostTransfer", true);
                collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                        .fileId(fileId)
                        .type(CollabMessageType.JOIN)
                        .userId(newHostId)
                        .payload(transferPayload)
                        .build());
            }
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("displayName", displayName);
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.LEAVE)
                .userId(userId)
                .payload(payload)
                .build());
    }

    @Transactional
    public void kickParticipant(Long fileId, String userIdToKick, String requesterUserId) {
        CollabSession session =
                sessionRepository.findByFileId(fileId).orElseThrow(() -> new NotFoundException("Session not found"));
        
        // Only the host can kick participants
        if (!requesterUserId.equals(session.getHostUserId())) {
            throw new BadRequestException("Only the session host can kick participants");
        }
        
        Participant kicked = participantRepository.findBySession_IdAndUserId(session.getId(), userIdToKick)
                .orElseThrow(() -> new NotFoundException("Participant not in session"));
        String kickedDisplayName = kicked.getDisplayName();
        
        participantRepository.deleteBySession_IdAndUserId(session.getId(), userIdToKick);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("kickedUserId", userIdToKick);
        payload.put("kickedDisplayName", kickedDisplayName);
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.KICK)
                .userId(requesterUserId)
                .payload(payload)
                .build());
    }

    @Transactional
    public void endSession(Long fileId, String requesterUserId, Long projectId) {
        CollabSession session =
                sessionRepository.findByFileId(fileId).orElseThrow(() -> new NotFoundException("Session not found"));
        
        // Only the host can end the session
        if (!requesterUserId.equals(session.getHostUserId())) {
            throw new BadRequestException("Only the session host can end the session");
        }
        
        List<Participant> participants = participantRepository.findAllBySession_Id(session.getId());
        String startedBy = participants.isEmpty() ? requesterUserId : participants.get(0).getUserId();
        String displayName = participants.stream()
                .filter(p -> p.getUserId().equals(requesterUserId))
                .findFirst()
                .map(Participant::getDisplayName)
                .orElse("Session");
        
        String participantsJson;
        try {
            List<Map<String, Object>> pList = participants.stream()
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("userId", p.getUserId());
                        map.put("displayName", p.getDisplayName());
                        map.put("joinedAt", p.getJoinedAt().toString());
                        return map;
                    })
                    .toList();
            participantsJson = objectMapper.writeValueAsString(pList);
        } catch (JsonProcessingException e) {
            participantsJson = "[]";
        }
        
        CollabHistory history = CollabHistory.builder()
                .sessionId(session.getId())
                .fileId(fileId)
                .projectId(projectId)
                .createdAt(session.getCreatedAt())
                .endedAt(Instant.now())
                .startedByUserId(startedBy)
                .endedByUserId(requesterUserId)
                .displayName(displayName)
                .participants(participantsJson)
                .build();
        historyRepository.save(history);
        
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.SESSION_END)
                .userId(requesterUserId)
                .payload(Map.of())
                .build());
        
        participantRepository.deleteAllBySession_Id(session.getId());
        sessionRepository.delete(session);
    }

    public CollabSessionResponse getSession(Long fileId) {
        CollabSession session =
                sessionRepository.findByFileId(fileId).orElseThrow(() -> new NotFoundException("Session not found"));
        return toResponse(fileId, session.getId(), session.getSessionUuid(), session.getHostUserId());
    }

    public java.util.Optional<CollabSessionResponse> getSessionByUuid(String sessionUuid) {
        return sessionRepository.findBySessionUuid(sessionUuid)
                .map(session -> toResponse(session.getFileId(), session.getId(), session.getSessionUuid(), session.getHostUserId()));
    }

    public List<AdminSessionView> listSessions() {
        return sessionRepository.findAll().stream()
                .map(session -> {
                    List<Participant> participants = participantRepository.findAllBySession_Id(session.getId());
                    List<AdminSessionView.ParticipantView> views = participants.stream()
                            .map(p -> new AdminSessionView.ParticipantView(p.getUserId(), p.getDisplayName(), p.getJoinedAt()))
                            .toList();
                    return new AdminSessionView(session.getId(), session.getFileId(), session.getCreatedAt(), views);
                })
                .toList();
    }

    @Transactional
    public void endSessionByAdmin(Long sessionId) {
        CollabSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        
        List<Participant> participants = participantRepository.findAllBySession_Id(session.getId());
        String startedBy = participants.isEmpty() ? "admin" : participants.get(0).getUserId();
        String displayName = "Session";
        
        String participantsJson;
        try {
            List<Map<String, Object>> pList = participants.stream()
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("userId", p.getUserId());
                        map.put("displayName", p.getDisplayName());
                        map.put("joinedAt", p.getJoinedAt().toString());
                        return map;
                    })
                    .toList();
            participantsJson = objectMapper.writeValueAsString(pList);
        } catch (JsonProcessingException e) {
            participantsJson = "[]";
        }
        
        CollabHistory history = CollabHistory.builder()
                .sessionId(session.getId())
                .fileId(session.getFileId())
                .projectId(null)
                .createdAt(session.getCreatedAt())
                .endedAt(Instant.now())
                .startedByUserId(startedBy)
                .endedByUserId("admin")
                .displayName(displayName)
                .participants(participantsJson)
                .build();
        historyRepository.save(history);
        
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
            .fileId(session.getFileId())
            .type(CollabMessageType.SESSION_END)
            .userId("admin")
            .payload(Map.of())
            .build());
        participantRepository.deleteAllBySession_Id(sessionId);
        sessionRepository.delete(session);
    }

    public List<CollabHistoryResponse> getHistoryByFileId(Long fileId) {
        return historyRepository.findByFileIdOrderByEndedAtDesc(fileId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public List<CollabHistoryResponse> getHistoryByProjectId(Long projectId) {
        return historyRepository.findByProjectIdOrderByEndedAtDesc(projectId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public List<CollabHistoryResponse> getHistoryByUserId(String userId) {
        return historyRepository.findAll().stream()
                .filter(h -> h.getStartedByUserId().equals(userId) || 
                          (h.getParticipants() != null && h.getParticipants().contains(userId)))
                .map(this::toHistoryResponse)
                .toList();
    }

    public Map<String, Object> getStats() {
        List<CollabSession> sessions = sessionRepository.findAll();
        List<CollabHistory> history = historyRepository.findAll();
        
        long totalSessions = sessions.size();
        long totalHistory = history.size();
        long activeParticipants = sessions.stream()
                .mapToLong(s -> participantRepository.findAllBySession_Id(s.getId()).size())
                .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", totalSessions);
        stats.put("totalHistory", totalHistory);
        stats.put("activeParticipants", activeParticipants);
        return stats;
    }

    private CollabHistoryResponse toHistoryResponse(CollabHistory history) {
        List<CollabHistoryResponse.HistoryParticipantView> pViews = List.of();
        if (history.getParticipants() != null && !history.getParticipants().isEmpty()) {
            try {
                var pList = objectMapper.readValue(history.getParticipants(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                pViews = pList.stream()
                        .map(p -> new CollabHistoryResponse.HistoryParticipantView(
                                (String) p.get("userId"),
                                (String) p.get("displayName"),
                                Instant.parse((String) p.get("joinedAt")))
                        )
                        .toList();
            } catch (JsonProcessingException e) {
                // ignore
            }
        }
        return new CollabHistoryResponse(
                history.getId(),
                history.getSessionId(),
                history.getFileId(),
                history.getProjectId(),
                history.getCreatedAt(),
                history.getEndedAt(),
                history.getStartedByUserId(),
                history.getEndedByUserId(),
                history.getDisplayName(),
                pViews
        );
    }

    private CollabSessionResponse toResponse(Long fileId, Long sessionId, String sessionUuid, String hostUserId) {
        List<Participant> participants = participantRepository.findAllBySession_Id(sessionId);
        List<CollabSessionResponse.ParticipantView> views = participants.stream()
                .map(p -> CollabSessionResponse.ParticipantView.builder()
                        .userId(p.getUserId())
                        .displayName(p.getDisplayName())
                        .build())
                .toList();
        return CollabSessionResponse.builder()
                .sessionId(sessionId)
                .sessionUuid(sessionUuid)
                .fileId(fileId)
                .hostUserId(hostUserId)
                .participants(views)
                .build();
    }
}
