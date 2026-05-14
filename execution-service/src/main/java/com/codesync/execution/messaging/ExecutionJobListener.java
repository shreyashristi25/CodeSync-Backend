package com.codesync.execution.messaging;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import com.codesync.execution.repository.ExecutionJobRepository;
import com.codesync.execution.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionJobListener {
    private final ExecutionJobRepository executionJobRepository;
    private final DockerExecutionService dockerExecutionService;

    @RabbitListener(queues = "${codesync.execution.queue}")
    @Transactional
    public void onJobMessage(String jobId) {
        ExecutionJob job = executionJobRepository
                .findByJobId(jobId)
                .orElseGet(() -> {
                    log.warn("Unknown job id {}", jobId);
                    return null;
                });
        if (job == null) {
            return;
        }
        if (job.getStatus() == JobStatus.CANCELLED) {
            log.info("Job {} was cancelled before execution started, skipping", jobId);
            return;
        }
        
        job.setStatus(JobStatus.RUNNING);
        executionJobRepository.save(job);
        
        try {
            DockerExecutionService.ExecutionRunResult result = dockerExecutionService.runInEphemeralEnvironment(
                    jobId, job.getCode(), job.getLanguage(), job.getStdin());

            ExecutionJob refreshed = executionJobRepository.findByJobId(jobId).orElse(null);
            if (refreshed == null || refreshed.getStatus() == JobStatus.CANCELLED) {
                log.info("Job {} was cancelled during execution, discarding results", jobId);
                return;
            }

            job.setOutput(result.stdout());
            job.setStderr(result.stderr());
            job.setExecutionTimeMs(0L);
            job.setMemoryUsedKb(0L);

            if (result.exitCode() == 124) {
                job.setError("Execution exceeded timeout");
                job.setStatus(JobStatus.TIMED_OUT);
            } else if (result.exitCode() != 0) {
                String detail = result.stdout().isBlank() ? result.stderr() : result.stdout();
                job.setError("exit " + result.exitCode() + (detail.isEmpty() ? "" : (": " + detail)));
                job.setStatus(JobStatus.FAILED);
            } else {
                job.setError(null);
                job.setStatus(JobStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.warn("Job failed {}", jobId, e);
            job.setStatus(JobStatus.FAILED);
            job.setError(e.getMessage());
        }
        executionJobRepository.save(job);
    }
}