package com.codesync.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminAccessRequestEntityTest {

    @Test
    void shouldCreateAdminAccessRequest() {
        AdminAccessRequest request = AdminAccessRequest.builder()
                .id(1L)
                .userId(1L)
                .email("test@test.com")
                .fullName("Test User")
                .status(AdminRequestStatus.PENDING)
                .build();

        assertNotNull(request);
        assertEquals(AdminRequestStatus.PENDING, request.getStatus());
    }

    @Test
    void shouldUpdateStatusToApproved() {
        AdminAccessRequest request = AdminAccessRequest.builder()
                .id(1L)
                .status(AdminRequestStatus.PENDING)
                .build();

        request.setStatus(AdminRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());

        assertEquals(AdminRequestStatus.APPROVED, request.getStatus());
        assertNotNull(request.getReviewedAt());
    }

    @Test
    void shouldUpdateStatusToDenied() {
        AdminAccessRequest request = AdminAccessRequest.builder()
                .id(1L)
                .status(AdminRequestStatus.PENDING)
                .build();

        request.setStatus(AdminRequestStatus.DENIED);
        request.setReviewedAt(LocalDateTime.now());

        assertEquals(AdminRequestStatus.DENIED, request.getStatus());
    }

    @Test
    void shouldCheckAdminRequestStatusValues() {
        assertEquals("PENDING", AdminRequestStatus.PENDING.name());
        assertEquals("APPROVED", AdminRequestStatus.APPROVED.name());
        assertEquals("DENIED", AdminRequestStatus.DENIED.name());
    }
}