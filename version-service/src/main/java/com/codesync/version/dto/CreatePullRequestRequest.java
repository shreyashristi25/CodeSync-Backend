package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePullRequestRequest(
        @NotNull Long repositoryId,
        @NotBlank String title,
        String description,
        @NotNull Long sourceBranchId,
        @NotNull Long targetBranchId,
        @NotNull Long authorId) {}