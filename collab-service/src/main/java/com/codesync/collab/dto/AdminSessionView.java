package com.codesync.collab.dto;

import java.time.Instant;
import java.util.List;

public record AdminSessionView(
        Long sessionId,
        Long fileId,
        Instant createdAt,
        List<ParticipantView> participants
) {
    public record ParticipantView(String userId, String displayName, Instant joinedAt) {}
}
