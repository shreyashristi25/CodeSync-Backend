package com.codesync.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteCollaboratorRequest(
    @NotNull Long userId,
    @NotBlank String message
) {}