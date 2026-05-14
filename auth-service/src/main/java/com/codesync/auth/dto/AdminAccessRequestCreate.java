package com.codesync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminAccessRequestCreate(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
