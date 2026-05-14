package com.codesync.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportedLanguageUpdateRequest(
        @NotBlank String displayName,
        @NotNull Boolean enabled,
        String dockerImage,
        String entryPoint
) {
}