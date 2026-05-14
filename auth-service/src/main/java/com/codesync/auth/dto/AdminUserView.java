package com.codesync.auth.dto;

import com.codesync.auth.entity.UserRole;
import com.codesync.auth.entity.UserStatus;
import java.time.LocalDateTime;

public record AdminUserView(
        Long id,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
}
