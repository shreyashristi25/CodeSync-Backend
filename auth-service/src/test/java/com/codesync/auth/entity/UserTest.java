package com.codesync.auth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldCreateUserBuilder() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .fullName("Test User")
                .passwordHash("hash123")
                .avatar("avatar.png")
                .bio("Test bio")
                .provider(AuthProvider.LOCAL)
                .role(UserRole.DEVELOPER)
                .status(UserStatus.ACTIVE)
                .build();

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("testuser", user.getUsername());
        assertEquals("Test User", user.getFullName());
        assertEquals(AuthProvider.LOCAL, user.getProvider());
        assertEquals(UserRole.DEVELOPER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldSetUserFields() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.SUSPENDED);

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }
}