package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBranchRequest(
        @NotNull Long projectId,
        @NotBlank String name,
        Long sourceBranchId) {}