package com.codesync.auth.dto;

public record AuthResponse(String token, Long userId, String email, String username, String fullName, String avatar, String bio, String role) {
}