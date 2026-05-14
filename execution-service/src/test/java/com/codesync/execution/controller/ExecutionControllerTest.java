package com.codesync.execution.controller;

import com.codesync.execution.dto.ExecutionJobResponse;
import com.codesync.execution.dto.SubmitJobRequest;
import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.service.ExecutionJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionControllerTest {

    @Mock
    private ExecutionJobService executionJobService;

    @InjectMocks
    private ExecutionController executionController;

    @Test
    void shouldSubmitJob() {
        SubmitJobRequest request = new SubmitJobRequest("print('test')", ExecutionLanguage.PYTHON, "");

        ExecutionJobResponse response = ExecutionJobResponse.builder()
                .jobId("job-123")
                .status(JobStatus.QUEUED)
                .build();

        when(executionJobService.submit(any(SubmitJobRequest.class))).thenReturn(response);

        ResponseEntity<ExecutionJobResponse> result = executionController.submit(request);

        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("job-123", result.getBody().getJobId());
    }

    @Test
    void shouldGetJobStatus() {
        String jobId = "job-123";
        ExecutionJobResponse response = ExecutionJobResponse.builder()
                .jobId(jobId)
                .status(JobStatus.COMPLETED)
                .output("test output")
                .build();

        when(executionJobService.get(jobId)).thenReturn(response);

        ResponseEntity<ExecutionJobResponse> result = executionController.status(jobId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(JobStatus.COMPLETED, result.getBody().getStatus());
    }

    @Test
    void shouldCancelJob() {
        String jobId = "job-123";
        ExecutionJobResponse response = ExecutionJobResponse.builder()
                .jobId(jobId)
                .status(JobStatus.CANCELLED)
                .build();

        when(executionJobService.cancel(jobId)).thenReturn(response);

        ResponseEntity<ExecutionJobResponse> result = executionController.cancel(jobId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(JobStatus.CANCELLED, result.getBody().getStatus());
    }
}