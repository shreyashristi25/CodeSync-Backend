package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionJobResponse;
import com.codesync.execution.dto.SubmitJobRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.exception.BadRequestException;
import com.codesync.execution.exception.NotFoundException;
import com.codesync.execution.messaging.ExecutionJobPublisher;
import com.codesync.execution.repository.ExecutionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionJobService {
    private final ExecutionJobRepository executionJobRepository;
    private final ExecutionJobPublisher executionJobPublisher;
    private final DockerExecutionService dockerExecutionService;

    @Transactional
    public ExecutionJobResponse submit(SubmitJobRequest request) {
        String jobId = ExecutionJob.newJobId();
        ExecutionJob job = ExecutionJob.builder()
                .jobId(jobId)
                .code(request.code())
                .language(request.language())
                .stdin(request.stdin())
                .status(JobStatus.QUEUED)
                .projectId(request.projectId())
                .fileName(request.fileName())
                .submittedAt(Instant.now())
                .build();
        executionJobRepository.save(job);
        executionJobPublisher.publishJobId(jobId);
        return ExecutionJobResponse.fromEntity(job);
    }

    public ExecutionJobResponse get(String jobId) {
        ExecutionJob job =
                executionJobRepository.findByJobId(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        return ExecutionJobResponse.fromEntity(job);
    }

    public List<ExecutionJobResponse> list(JobStatus status) {
        if (status == null) {
            return executionJobRepository.findAll().stream()
                    .map(ExecutionJobResponse::fromEntity)
                    .toList();
        }
        return executionJobRepository.findByStatus(status).stream()
                .map(ExecutionJobResponse::fromEntity)
                .toList();
    }

    public List<ExecutionJobResponse> getHistory() {
        return executionJobRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(ExecutionJobResponse::fromEntity)
                .toList();
    }

    /**
     * Cancel a running or pending job.
     * Marks status as CANCELLED in the DB and hard-kills the OS process if it is active.
     */
    @Transactional
    public ExecutionJobResponse cancel(String jobId) {
        ExecutionJob job =
                executionJobRepository.findByJobId(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        if (job.getStatus() == JobStatus.COMPLETED
                || job.getStatus() == JobStatus.FAILED
                || job.getStatus() == JobStatus.CANCELLED) {
            throw new BadRequestException("Job is already in terminal state: " + job.getStatus());
        }
        job.setStatus(JobStatus.CANCELLED);
        executionJobRepository.save(job);
        // Hard-kill the OS process (no-op if already finished)
        dockerExecutionService.cancelJob(jobId);
        return ExecutionJobResponse.fromEntity(job);
    }

    public Map<String, Object> getStats() {
        List<ExecutionJob> allJobs = executionJobRepository.findAll();
        long total = allJobs.size();
        Map<JobStatus, Long> statusBreakdown = allJobs.stream()
                .collect(Collectors.groupingBy(ExecutionJob::getStatus, Collectors.counting()));

        Map<String, Long> languageBreakdown = allJobs.stream()
                .collect(Collectors.groupingBy(job -> job.getLanguage().name(), Collectors.counting()));

        long avgExecutionTime = 0;
        long countWithTime = allJobs.stream().filter(job -> job.getExecutionTimeMs() != null).count();
        if (countWithTime > 0) {
            long sumTime = allJobs.stream().filter(job -> job.getExecutionTimeMs() != null)
                    .mapToLong(ExecutionJob::getExecutionTimeMs).sum();
            avgExecutionTime = sumTime / countWithTime;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", total);
        stats.put("statusBreakdown", statusBreakdown);
        stats.put("languageBreakdown", languageBreakdown);
        stats.put("avgExecutionTime", avgExecutionTime);
        return stats;
    }
}
