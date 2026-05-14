package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestoreSnapshotRequest(
        @NotNull Long fileId,
        @NotBlank String snapshotHash,
        Long authorId,
        String commitMessage) {}