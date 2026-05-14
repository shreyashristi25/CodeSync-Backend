package com.codesync.version.dto;

import jakarta.validation.constraints.NotNull;

public record MergePullRequestRequest(
        @NotNull Long pullRequestId,
        @NotNull Long authorId) {}