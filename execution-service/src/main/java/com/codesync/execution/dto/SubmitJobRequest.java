package com.codesync.execution.dto;

import com.codesync.execution.entity.ExecutionLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitJobRequest(
        @NotBlank String code,
        @NotNull ExecutionLanguage language,
        String stdin,
        Long projectId,
        String fileName) {}

