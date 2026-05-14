package com.codesync.audit.service;

import com.codesync.audit.entity.AuditLog;
import com.codesync.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void shouldLogActionWithAllParameters() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        auditLogService.logAction(1L, "test@test.com", "USER_LOGIN", "user", 1L, 
                "oldValue", "newValue", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("test@test.com", saved.getUserEmail());
        assertEquals("USER_LOGIN", saved.getAction());
        assertEquals("user", saved.getResourceType());
        assertEquals(1L, saved.getResourceId());
        assertEquals("\"oldValue\"", saved.getPreviousValue());
        assertEquals("\"newValue\"", saved.getNewValue());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void shouldLogActionWithNullPreviousAndNewValue() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        auditLogService.logAction(1L, "test@test.com", "PROJECT_CREATE", "project", 1L, 
                null, null, null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void shouldGetLogsForUser() {
        AuditLog log1 = AuditLog.builder().id(1L).userId(1L).action("USER_LOGIN").build();
        AuditLog log2 = AuditLog.builder().id(2L).userId(1L).action("PROJECT_CREATE").build();
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(log1, log2));

        List<AuditLog> result = auditLogService.getLogsForUser(1L);

        assertEquals(2, result.size());
        verify(auditLogRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void shouldGetLogsForResource() {
        AuditLog log1 = AuditLog.builder().id(1L).resourceType("project").resourceId(1L).build();
        when(auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc("project", 1L))
                .thenReturn(List.of(log1));

        List<AuditLog> result = auditLogService.getLogsForResource("project", 1L);

        assertEquals(1, result.size());
        assertEquals("project", result.get(0).getResourceType());
    }

    @Test
    void shouldGetLogsWithPagination() {
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().id(1L).build()));
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getLogs(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldGetLogsByAction() {
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().id(1L).action("USER_LOGIN").build()));
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findByActionOrderByCreatedAtDesc("USER_LOGIN", pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getLogsByAction("USER_LOGIN", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("USER_LOGIN", result.getContent().get(0).getAction());
    }
}