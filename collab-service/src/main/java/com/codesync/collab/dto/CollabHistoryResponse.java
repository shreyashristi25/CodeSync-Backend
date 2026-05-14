package com.codesync.collab.dto;

import java.time.Instant;
import java.util.List;

public record CollabHistoryResponse(
        Long id,
        Long sessionId,
        Long fileId,
        Long projectId,
        Instant createdAt,
        Instant endedAt,
        String startedByUserId,
        String endedByUserId,
        String displayName,
        List<HistoryParticipantView> participants
) {
    public record HistoryParticipantView(
            String userId,
            String displayName,
            Instant joinedAt
    ) {}
}