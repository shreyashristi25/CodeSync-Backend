package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRepositoryRequest(
        @NotNull Long projectId,
        @NotBlank String name,
        Boolean isPublic) {}