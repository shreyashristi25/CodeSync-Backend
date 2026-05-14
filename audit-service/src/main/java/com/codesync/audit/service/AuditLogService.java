package com.codesync.audit.service;

import com.codesync.audit.entity.AuditLog;
import com.codesync.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    @Transactional
    public void logAction(Long userId, String userEmail, String action, String resourceType, Long resourceId, 
                     Object previousValue, Object newValue, String ipAddress) {
        try {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .previousValue(previousValue != null ? objectMapper.writeValueAsString(previousValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize audit log value: " + e.getMessage());
        }
    }

    public List<AuditLog> getLogsForUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<AuditLog> getLogsForResource(String resourceType, Long resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId);
    }

    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }
}