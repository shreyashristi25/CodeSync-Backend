package com.codesync.execution.controller;

import com.codesync.execution.dto.ExecutionJobResponse;
import com.codesync.execution.dto.SubmitJobRequest;
import com.codesync.execution.service.ExecutionJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution/jobs")
@RequiredArgsConstructor
public class ExecutionController {
    private final ExecutionJobService executionJobService;

    @PostMapping
    public ResponseEntity<ExecutionJobResponse> submit(@Valid @RequestBody SubmitJobRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(executionJobService.submit(request));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ExecutionJobResponse> status(@PathVariable String jobId) {
        return ResponseEntity.ok(executionJobService.get(jobId));
    }

    /**
     * Cancel a PENDING or RUNNING job.
     * Returns the updated job response with status CANCELLED.
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ExecutionJobResponse> cancel(@PathVariable String jobId) {
        return ResponseEntity.ok(executionJobService.cancel(jobId));
    }
}
