package com.codesync.collab.controller;

import com.codesync.collab.dto.CollabBroadcastEnvelope;
import com.codesync.collab.dto.CollabHistoryResponse;
import com.codesync.collab.dto.CollabMessageType;
import com.codesync.collab.dto.CollabSessionResponse;
import com.codesync.collab.dto.CursorMovePayload;
import com.codesync.collab.dto.DocumentEditPayload;
import com.codesync.collab.dto.JoinCollabRequest;
import com.codesync.collab.dto.LeaveCollabRequest;
import com.codesync.collab.redis.CollabRedisPublisher;
import com.codesync.collab.service.CollabService;
import com.codesync.collab.service.OTService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Validated
public class CollabController {
    private final CollabService collabService;
    private final CollabRedisPublisher collabRedisPublisher;
    private final OTService otService;

    @PostMapping("/api/collab/files/{fileId}/join")
    @ResponseBody
    public ResponseEntity<CollabSessionResponse> join(
            @PathVariable Long fileId, @Valid @RequestBody JoinCollabRequest request) {
        return ResponseEntity.ok(collabService.join(fileId, request));
    }

    @PostMapping("/api/collab/files/{fileId}/start")
    @ResponseBody
    public ResponseEntity<CollabSessionResponse> start(
            @PathVariable Long fileId, @Valid @RequestBody JoinCollabRequest request) {
        return ResponseEntity.ok(collabService.startSession(fileId, request));
    }

    @PostMapping("/api/collab/files/{fileId}/leave")
    @ResponseBody
    public ResponseEntity<Void> leave(
            @PathVariable Long fileId, @Valid @RequestBody LeaveCollabRequest request) {
        collabService.leave(fileId, request.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/collab/files/{fileId}/kick")
    @ResponseBody
    public ResponseEntity<Void> kick(
            @PathVariable Long fileId, 
            @RequestBody Map<String, String> request) {
        String userIdToKick = request.get("userIdToKick");
        String requesterUserId = request.get("requesterUserId");
        collabService.kickParticipant(fileId, userIdToKick, requesterUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/collab/files/{fileId}/end")
    @ResponseBody
    public ResponseEntity<Void> endSession(
            @PathVariable Long fileId, 
            @RequestBody Map<String, Object> request) {
        String requesterUserId = (String) request.get("requesterUserId");
        Long projectId = request.get("projectId") != null ? 
                ((Number) request.get("projectId")).longValue() : null;
        collabService.endSession(fileId, requesterUserId, projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/collab/files/{fileId}/history")
    @ResponseBody
    public ResponseEntity<List<CollabHistoryResponse>> getFileHistory(@PathVariable Long fileId) {
        return ResponseEntity.ok(collabService.getHistoryByFileId(fileId));
    }

    @GetMapping("/api/collab/projects/{projectId}/history")
    @ResponseBody
    public ResponseEntity<List<CollabHistoryResponse>> getProjectHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(collabService.getHistoryByProjectId(projectId));
    }

    @GetMapping("/api/collab/users/{userId}/history")
    @ResponseBody
    public ResponseEntity<List<CollabHistoryResponse>> getUserHistory(@PathVariable String userId) {
        return ResponseEntity.ok(collabService.getHistoryByUserId(userId));
    }

    @GetMapping("/api/collab/files/{fileId}/session")
    @ResponseBody
    public ResponseEntity<CollabSessionResponse> getSession(@PathVariable Long fileId) {
        return ResponseEntity.ok(collabService.getSession(fileId));
    }

    @GetMapping("/api/collab/join/{sessionUuid}")
    @ResponseBody
    public ResponseEntity<CollabSessionResponse> joinByUuid(@PathVariable String sessionUuid) {
        return collabService.getSessionByUuid(sessionUuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @MessageMapping("/collab/{fileId}/edit")
    public void broadcastEdit(@DestinationVariable Long fileId, @Valid @Payload DocumentEditPayload payload) {
        OTService.DocumentOperation op = OTService.DocumentOperation.builder()
                .userId(payload.getUserId())
                .type(OTService.OperationType.valueOf(payload.getOperationType()))
                .position(payload.getPosition())
                .length(payload.getLength())
                .text(payload.getText())
                .timestamp(System.currentTimeMillis())
                .build();
        
        OTService.DocumentOperation transformed = otService.applyOperation(fileId, op);
        
        Map<String, Object> body = new HashMap<>();
        body.put("operationType", transformed.getType().name());
        body.put("position", transformed.getPosition());
        body.put("length", transformed.getLength());
        body.put("text", transformed.getText());
        body.put("timestamp", transformed.getTimestamp());
        
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.EDIT)
                .userId(payload.getUserId())
                .payload(body)
                .build());
    }

    @MessageMapping("/collab/{fileId}/cursor")
    public void broadcastCursor(@DestinationVariable Long fileId, @Valid @Payload CursorMovePayload payload) {
        Map<String, Object> body = new HashMap<>();
        body.put("lineNumber", payload.getLineNumber());
        body.put("column", payload.getColumn());
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.CURSOR)
                .userId(payload.getUserId())
                .payload(body)
                .build());
    }

    @MessageMapping("/collab/{fileId}/chat")
    public void broadcastChat(@DestinationVariable Long fileId, @Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String displayName = payload.get("displayName");
        String message = payload.get("message");
        
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", displayName);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        
        collabRedisPublisher.publish(CollabBroadcastEnvelope.builder()
                .fileId(fileId)
                .type(CollabMessageType.CHAT)
                .userId(userId)
                .payload(body)
                .build());
    }
}
