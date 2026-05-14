package com.codesync.audit.controller;

import com.codesync.audit.entity.AuditLog;
import com.codesync.audit.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    @Test
    void shouldGetLogs() {
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().id(1L).build()));
        when(auditLogService.getLogs(any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<AuditLog>> result = auditLogController.getLogs(0, 20);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void shouldGetUserLogs() {
        List<AuditLog> logs = List.of(AuditLog.builder().id(1L).userId(1L).build());
        when(auditLogService.getLogsForUser(1L)).thenReturn(logs);

        ResponseEntity<?> result = auditLogController.getUserLogs(1L);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }

    @Test
    void shouldGetResourceLogs() {
        List<AuditLog> logs = List.of(AuditLog.builder().id(1L).resourceType("project").resourceId(1L).build());
        when(auditLogService.getLogsForResource("project", 1L)).thenReturn(logs);

        ResponseEntity<?> result = auditLogController.getResourceLogs("project", 1L);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }

    @Test
    void shouldGetLogsByAction() {
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().id(1L).action("USER_LOGIN").build()));
        when(auditLogService.getLogsByAction(eq("USER_LOGIN"), any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<AuditLog>> result = auditLogController.getLogsByAction("USER_LOGIN", 0, 20);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
    }
}