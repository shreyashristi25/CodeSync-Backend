package com.codesync.execution.controller;

import com.codesync.execution.dto.ExecutionJobResponse;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.service.ExecutionJobService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution/admin")
@RequiredArgsConstructor
public class ExecutionAdminController {
    private final ExecutionJobService executionJobService;

    @GetMapping("/jobs")
    public ResponseEntity<List<ExecutionJobResponse>> listJobs(@RequestParam(required = false) JobStatus status) {
        return ResponseEntity.ok(executionJobService.list(status));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<ExecutionJobResponse> cancelJob(@PathVariable String jobId) {
        return ResponseEntity.ok(executionJobService.cancel(jobId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getExecutionStats() {
        return ResponseEntity.ok(executionJobService.getStats());
    }

    @GetMapping("/history")
    public ResponseEntity<List<ExecutionJobResponse>> getHistory() {
        return ResponseEntity.ok(executionJobService.getHistory());
    }
}
