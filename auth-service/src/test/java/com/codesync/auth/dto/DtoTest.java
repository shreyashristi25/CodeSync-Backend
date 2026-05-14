package com.codesync.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AuthResponseTest {

    @Test
    void shouldCreateAuthResponse() {
        AuthResponse response = new AuthResponse("token123", 1L, "test@example.com", "testuser", "Test User", "avatar.png", "bio", "DEVELOPER");
        
        assertNotNull(response);
        assertEquals("token123", response.token());
        assertEquals(1L, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("testuser", response.username());
        assertEquals("Test User", response.fullName());
        assertEquals("DEVELOPER", response.role());
    }
}

class LoginRequestTest {

    @Test
    void shouldCreateLoginRequest() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        assertNotNull(request);
        assertEquals("test@example.com", request.email());
        assertEquals("password123", request.password());
    }
}

class RegisterRequestTest {

    @Test
    void shouldCreateRegisterRequest() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "Test User", "password123", "avatar", "bio");
        
        assertNotNull(request);
        assertEquals("test@example.com", request.email());
        assertEquals("testuser", request.username());
        assertEquals("Test User", request.fullName());
        assertEquals("password123", request.password());
    }
}

class AdminUserViewTest {

    @Test
    void shouldCreateAdminUserView() {
        AdminUserView view = new AdminUserView(1L, "test@example.com", "Test User", com.codesync.auth.entity.UserRole.DEVELOPER, com.codesync.auth.entity.UserStatus.ACTIVE, null);
        
        assertNotNull(view);
        assertEquals(1L, view.id());
        assertEquals("test@example.com", view.email());
    }
}