package com.codesync.collab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesync.collab.dto.AdminSessionView;
import com.codesync.collab.dto.CollabHistoryResponse;
import com.codesync.collab.dto.CollabMessageType;
import com.codesync.collab.dto.CollabSessionResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollabServiceTest {

    @Mock
    private CollabSessionRepository sessionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private CollabHistoryRepository historyRepository;

    @Mock
    private CollabRedisPublisher collabRedisPublisher;

    private CollabService collabService;

    @BeforeEach
    void setUp() {
        collabService = new CollabService(sessionRepository, participantRepository, historyRepository, collabRedisPublisher, new ObjectMapper());
    }

    @Test
    void shouldCreateSessionOnJoin() {
        when(sessionRepository.findByFileId(10L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> {
            CollabSession s = invocation.getArgument(0);
            s.setId(99L);
            return s;
        });
        when(participantRepository.findBySession_IdAndUserId(99L, "u1")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.findAllBySession_Id(99L))
                .thenReturn(List.of(Participant.builder()
                        .id(1L)
                        .userId("u1")
                        .displayName("A")
                        .joinedAt(Instant.now())
                        .build()));
        CollabSessionResponse response = collabService.join(10L, new JoinCollabRequest("u1", "A"));
        assertEquals(99L, response.getSessionId());
        assertEquals(10L, response.getFileId());
        verify(collabRedisPublisher)
                .publish(argThat(env -> env.getType() == CollabMessageType.JOIN && env.getFileId().equals(10L)));
    }

    @Test
    void shouldReturnExistingSessionOnJoin() {
        CollabSession session = CollabSession.builder()
                .id(1L)
                .fileId(2L)
                .sessionUuid("abc-123")
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .participants(new ArrayList<>())
                .build();
        when(sessionRepository.findByFileId(2L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(CollabSession.class))).thenReturn(session);
        when(participantRepository.findBySession_IdAndUserId(1L, "u1")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.findAllBySession_Id(1L))
                .thenReturn(List.of(Participant.builder()
                        .id(1L)
                        .userId("u1")
                        .displayName("A")
                        .joinedAt(Instant.now())
                        .build()));
        CollabSessionResponse response = collabService.join(2L, new JoinCollabRequest("u1", "A"));
        assertEquals(1L, response.getSessionId());
    }

    @Test
    void shouldLeaveSession() {
        CollabSession session = CollabSession.builder().id(3L).fileId(4L).createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(4L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(3L, "u2"))
                .thenReturn(Optional.of(Participant.builder()
                        .id(2L)
                        .session(session)
                        .userId("u2")
                        .displayName("B")
                        .joinedAt(Instant.now())
                        .build()));
        collabService.leave(4L, "u2");
        verify(participantRepository).deleteBySession_IdAndUserId(3L, "u2");
        verify(collabRedisPublisher)
                .publish(argThat(env -> env.getType() == CollabMessageType.LEAVE && "u2".equals(env.getUserId())));
    }

    @Test
    void shouldFailLeaveWhenMissingParticipant() {
        CollabSession session = CollabSession.builder().id(3L).fileId(4L).createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(4L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(3L, "ghost")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> collabService.leave(4L, "ghost"));
    }

    @Test
    void shouldKickParticipant() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(5L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(1L, "requester")).thenReturn(Optional.of(Participant.builder().id(1L).build()));
        when(participantRepository.findBySession_IdAndUserId(1L, "toKick")).thenReturn(Optional.of(Participant.builder().id(2L).build()));

        collabService.kickParticipant(5L, "toKick", "requester");

        verify(participantRepository).deleteBySession_IdAndUserId(1L, "toKick");
        verify(collabRedisPublisher).publish(argThat(env -> env.getType() == CollabMessageType.KICK));
    }

    @Test
    void shouldFailKickWhenRequesterNotInSession() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(5L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(1L, "requester")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> collabService.kickParticipant(5L, "toKick", "requester"));
    }

    @Test
    void shouldFailKickWhenParticipantNotInSession() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(5L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(1L, "requester")).thenReturn(Optional.of(Participant.builder().id(1L).build()));
        when(participantRepository.findBySession_IdAndUserId(1L, "toKick")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> collabService.kickParticipant(5L, "toKick", "requester"));
    }

    @Test
    void shouldEndSession() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).sessionUuid("uuid-1").createdAt(Instant.now()).build();
        List<Participant> participants = List.of(Participant.builder().id(1L).userId("user1").displayName("User1").joinedAt(Instant.now()).build());

        when(sessionRepository.findByFileId(5L)).thenReturn(Optional.of(session));
        when(participantRepository.findBySession_IdAndUserId(1L, "user1")).thenReturn(Optional.of(participants.get(0)));
        when(participantRepository.findAllBySession_Id(1L)).thenReturn(participants);
        when(historyRepository.save(any(CollabHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        collabService.endSession(5L, "user1", 10L);

        verify(participantRepository).deleteAllBySession_Id(1L);
        verify(sessionRepository).delete(session);
        verify(collabRedisPublisher).publish(argThat(env -> env.getType() == CollabMessageType.SESSION_END));
    }

    @Test
    void shouldGetSession() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).sessionUuid("uuid-1").createdAt(Instant.now()).build();
        when(sessionRepository.findByFileId(5L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllBySession_Id(1L)).thenReturn(List.of());

        CollabSessionResponse response = collabService.getSession(5L);

        assertEquals(1L, response.getSessionId());
        assertEquals("uuid-1", response.getSessionUuid());
    }

    @Test
    void shouldGetSessionByUuid() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).sessionUuid("uuid-1").createdAt(Instant.now()).build();
        when(sessionRepository.findBySessionUuid("uuid-1")).thenReturn(Optional.of(session));
        when(participantRepository.findAllBySession_Id(1L)).thenReturn(List.of());

        Optional<CollabSessionResponse> response = collabService.getSessionByUuid("uuid-1");

        assertTrue(response.isPresent());
        assertEquals(1L, response.get().getSessionId());
    }

    @Test
    void shouldListSessions() {
        CollabSession session = CollabSession.builder().id(1L).fileId(5L).createdAt(Instant.now()).build();
        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(participantRepository.findAllBySession_Id(1L)).thenReturn(List.of(Participant.builder().userId("u1").displayName("User1").joinedAt(Instant.now()).build()));

        List<AdminSessionView> result = collabService.listSessions();

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetHistoryByFileId() {
        CollabHistory history = CollabHistory.builder().id(1L).fileId(5L).participants("[]").build();
        when(historyRepository.findByFileIdOrderByEndedAtDesc(5L)).thenReturn(List.of(history));

        List<CollabHistoryResponse> result = collabService.getHistoryByFileId(5L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetHistoryByProjectId() {
        CollabHistory history = CollabHistory.builder().id(1L).projectId(10L).participants("[]").build();
        when(historyRepository.findByProjectIdOrderByEndedAtDesc(10L)).thenReturn(List.of(history));

        List<CollabHistoryResponse> result = collabService.getHistoryByProjectId(10L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetStats() {
        CollabSession session1 = CollabSession.builder().id(1L).fileId(5L).build();
        CollabSession session2 = CollabSession.builder().id(2L).fileId(6L).build();
        when(sessionRepository.findAll()).thenReturn(List.of(session1, session2));
        when(historyRepository.findAll()).thenReturn(List.of());
        when(participantRepository.findAllBySession_Id(1L)).thenReturn(List.of(Participant.builder().build()));
        when(participantRepository.findAllBySession_Id(2L)).thenReturn(List.of(Participant.builder().build(), Participant.builder().build()));

        var stats = collabService.getStats();

        assertEquals(2L, stats.get("totalSessions"));
        assertEquals(3L, stats.get("activeParticipants"));
    }
}
