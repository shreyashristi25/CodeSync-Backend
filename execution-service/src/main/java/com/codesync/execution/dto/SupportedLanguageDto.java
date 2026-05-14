package com.codesync.execution.dto;

public record SupportedLanguageDto(
        Long id,
        String code,
        String displayName,
        boolean enabled,
        String dockerImage,
        String entryPoint
) {
}