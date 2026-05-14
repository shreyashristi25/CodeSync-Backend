package com.codesync.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminBroadcastRequest(
        @NotEmpty List<Long> userIds,
        @NotBlank String type,
        @NotBlank String message
) {
}
