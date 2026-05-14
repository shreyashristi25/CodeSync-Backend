package com.codesync.collab.controller;

import com.codesync.collab.dto.AdminSessionView;
import com.codesync.collab.service.CollabService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collab/admin")
@RequiredArgsConstructor
public class CollabAdminController {
    private final CollabService collabService;

    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionView>> listSessions() {
        return ResponseEntity.ok(collabService.listSessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> endSession(@PathVariable Long sessionId) {
        collabService.endSessionByAdmin(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCollabStats() {
        return ResponseEntity.ok(collabService.getStats());
    }
}
