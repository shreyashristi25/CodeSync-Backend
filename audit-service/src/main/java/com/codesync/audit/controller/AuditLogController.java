package com.codesync.audit.controller;

import com.codesync.audit.entity.AuditLog;
import com.codesync.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<?> getUserLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.getLogsForUser(userId));
    }

    @GetMapping("/logs/resource/{type}/{id}")
    public ResponseEntity<?> getResourceLogs(
            @PathVariable String type,
            @PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getLogsForResource(type, id));
    }

    @GetMapping("/logs/action/{action}")
    public ResponseEntity<Page<AuditLog>> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getLogsByAction(action, PageRequest.of(page, size)));
    }
}