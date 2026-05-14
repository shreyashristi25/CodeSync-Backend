package com.codesync.project.dto;

import java.time.LocalDateTime;

public record InviteResponse(
    Long id,
    Long projectId,
    Long invitedByUserId,
    Long userId,
    String status,
    String message,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    String inviteLink
) {}