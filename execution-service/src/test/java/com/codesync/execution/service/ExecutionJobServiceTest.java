package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionJobResponse;
import com.codesync.execution.dto.SubmitJobRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.exception.BadRequestException;
import com.codesync.execution.exception.NotFoundException;
import com.codesync.execution.messaging.ExecutionJobPublisher;
import com.codesync.execution.repository.ExecutionJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionJobServiceTest {

    @Mock
    private ExecutionJobRepository executionJobRepository;

    @Mock
    private ExecutionJobPublisher executionJobPublisher;

    @Mock
    private DockerExecutionService dockerExecutionService;

    @InjectMocks
    private ExecutionJobService executionJobService;

    @Test
    void shouldSubmitJob() {
        when(executionJobRepository.save(any(ExecutionJob.class))).thenAnswer(invocation -> {
            ExecutionJob job = invocation.getArgument(0);
            job.setId(1L);
            return job;
        });

        SubmitJobRequest request = new SubmitJobRequest("print('hello')", ExecutionLanguage.PYTHON, null);
        ExecutionJobResponse result = executionJobService.submit(request);

        assertNotNull(result.getJobId());
        assertEquals(ExecutionLanguage.PYTHON, result.getLanguage());
        assertEquals(JobStatus.QUEUED, result.getStatus());
        verify(executionJobPublisher).publishJobId(any(String.class));
    }

    @Test
    void shouldGetJob() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("job-123")
                .code("print('test')")
                .language(ExecutionLanguage.PYTHON)
                .status(JobStatus.COMPLETED)
                .build();
        when(executionJobRepository.findByJobId("job-123")).thenReturn(Optional.of(job));

        ExecutionJobResponse result = executionJobService.get("job-123");

        assertEquals("job-123", result.getJobId());
        assertEquals(JobStatus.COMPLETED, result.getStatus());
    }

    @Test
    void shouldThrowWhenGettingNonExistentJob() {
        when(executionJobRepository.findByJobId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> executionJobService.get("nonexistent"));
    }

    @Test
    void shouldListAllJobs() {
        when(executionJobRepository.findAll()).thenReturn(List.of(
                ExecutionJob.builder().id(1L).jobId("job-1").language(ExecutionLanguage.PYTHON).status(JobStatus.COMPLETED).build(),
                ExecutionJob.builder().id(2L).jobId("job-2").language(ExecutionLanguage.JAVA).status(JobStatus.QUEUED).build()
        ));

        List<ExecutionJobResponse> result = executionJobService.list(null);

        assertEquals(2, result.size());
    }

    @Test
    void shouldListJobsByStatus() {
        when(executionJobRepository.findByStatus(JobStatus.COMPLETED)).thenReturn(List.of(
                ExecutionJob.builder().id(1L).jobId("job-1").language(ExecutionLanguage.PYTHON).status(JobStatus.COMPLETED).build()
        ));

        List<ExecutionJobResponse> result = executionJobService.list(JobStatus.COMPLETED);

        assertEquals(1, result.size());
        assertEquals(JobStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void shouldCancelJob() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("job-123")
                .status(JobStatus.QUEUED)
                .build();
        when(executionJobRepository.findByJobId("job-123")).thenReturn(Optional.of(job));
        when(executionJobRepository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutionJobResponse result = executionJobService.cancel("job-123");

        assertEquals(JobStatus.CANCELLED, result.getStatus());
        verify(dockerExecutionService).cancelJob("job-123");
    }

    @Test
    void shouldThrowWhenCancellingCompletedJob() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("job-123")
                .language(ExecutionLanguage.PYTHON)
                .status(JobStatus.COMPLETED)
                .build();
        when(executionJobRepository.findByJobId("job-123")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class, () -> executionJobService.cancel("job-123"));
    }

    @Test
    void shouldThrowWhenCancellingFailedJob() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("job-123")
                .language(ExecutionLanguage.PYTHON)
                .status(JobStatus.FAILED)
                .build();
        when(executionJobRepository.findByJobId("job-123")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class, () -> executionJobService.cancel("job-123"));
    }

    @Test
    void shouldThrowWhenCancellingCancelledJob() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("job-123")
                .language(ExecutionLanguage.PYTHON)
                .status(JobStatus.CANCELLED)
                .build();
        when(executionJobRepository.findByJobId("job-123")).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class, () -> executionJobService.cancel("job-123"));
    }

    @Test
    void shouldGetStats() {
        when(executionJobRepository.findAll()).thenReturn(List.of(
                ExecutionJob.builder().id(1L).language(ExecutionLanguage.PYTHON).status(JobStatus.COMPLETED).build(),
                ExecutionJob.builder().id(2L).language(ExecutionLanguage.PYTHON).status(JobStatus.FAILED).build(),
                ExecutionJob.builder().id(3L).language(ExecutionLanguage.JAVA).status(JobStatus.QUEUED).build()
        ));

        var stats = executionJobService.getStats();

        assertEquals(3L, stats.get("totalExecutions"));
    }
}