package com.codesync.auth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AdminAccessRequestTest {

    @Test
    void shouldCreateAdminAccessRequest() {
        AdminAccessRequest request = AdminAccessRequest.builder()
                .id(1L)
                .userId(100L)
                .email("user@example.com")
                .fullName("Test User")
                .status(AdminRequestStatus.PENDING)
                .build();

        assertNotNull(request);
        assertEquals(100L, request.getUserId());
        assertEquals("user@example.com", request.getEmail());
        assertEquals(AdminRequestStatus.PENDING, request.getStatus());
    }

    @Test
    void shouldSetAdminAccessRequestFields() {
        AdminAccessRequest request = new AdminAccessRequest();
        request.setId(2L);
        request.setUserId(200L);
        request.setEmail("admin@example.com");
        request.setFullName("Admin User");
        request.setStatus(AdminRequestStatus.APPROVED);

        assertEquals(2L, request.getId());
        assertEquals(200L, request.getUserId());
        assertEquals(AdminRequestStatus.APPROVED, request.getStatus());
    }
}

class UserRoleTest {

    @Test
    void shouldCheckUserRoleValues() {
        assertEquals("DEVELOPER", UserRole.DEVELOPER.name());
        assertEquals("ADMIN", UserRole.ADMIN.name());
    }
}

class UserStatusTest {

    @Test
    void shouldCheckUserStatusValues() {
        assertEquals("ACTIVE", UserStatus.ACTIVE.name());
        assertEquals("SUSPENDED", UserStatus.SUSPENDED.name());
    }
}

class AuthProviderTest {

    @Test
    void shouldCheckAuthProviderValues() {
        assertEquals("LOCAL", AuthProvider.LOCAL.name());
        assertEquals("GOOGLE", AuthProvider.GOOGLE.name());
        assertEquals("GITHUB", AuthProvider.GITHUB.name());
    }
}