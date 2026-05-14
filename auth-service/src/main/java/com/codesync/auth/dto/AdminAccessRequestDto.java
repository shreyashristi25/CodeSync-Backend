package com.codesync.auth.dto;

import com.codesync.auth.entity.AdminRequestStatus;
import java.time.LocalDateTime;

public record AdminAccessRequestDto(
        Long id,
        Long userId,
        String email,
        String fullName,
        AdminRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewedByEmail
) {
}
