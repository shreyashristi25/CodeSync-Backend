package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTagRequest(
        @NotBlank(message = "Tag name is required")
        @Pattern(regexp = "^v\\d+\\.\\d+(\\.\\d+)?(-[a-zA-Z0-9]+)?$", 
                message = "Tag must follow semantic versioning (e.g., v1.0, v1.0.0, v1.0.0-beta)")
        String name,
        String commitHash,
        @NotBlank(message = "Description is required")
        String description
) {}