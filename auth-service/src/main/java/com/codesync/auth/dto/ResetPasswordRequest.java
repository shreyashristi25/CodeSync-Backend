package com.codesync.auth.dto;

public record ResetPasswordRequest(String email, String token, String newPassword) {}