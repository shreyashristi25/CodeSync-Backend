package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSnapshotRequest(
        @NotNull Long fileId,
        @NotBlank String fullContent,
        String parentHash,
        String commitMessage,
        Long authorId,
        Long branchId) {}
