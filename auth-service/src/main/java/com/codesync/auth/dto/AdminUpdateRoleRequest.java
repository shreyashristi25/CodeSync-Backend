package com.codesync.auth.dto;

import com.codesync.auth.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateRoleRequest(@NotNull UserRole role) {
}
