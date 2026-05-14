package com.codesync.auth.dto;

import com.codesync.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateStatusRequest(@NotNull UserStatus status) {
}
