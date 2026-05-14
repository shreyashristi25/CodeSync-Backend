package com.codesync.execution.dto;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.JobStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionJobResponse {
    private String jobId;
    private ExecutionLanguage language;
    private JobStatus status;
    private String output;
    private String stderr;
    private String error;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    private Long projectId;
    private String fileName;
    private java.time.Instant submittedAt;

    public static ExecutionJobResponse fromEntity(ExecutionJob job) {
        return ExecutionJobResponse.builder()
                .jobId(job.getJobId())
                .language(job.getLanguage())
                .status(job.getStatus())
                .output(job.getOutput())
                .stderr(job.getStderr())
                .error(job.getError())
                .executionTimeMs(job.getExecutionTimeMs())
                .memoryUsedKb(job.getMemoryUsedKb())
                .projectId(job.getProjectId())
                .fileName(job.getFileName())
                .submittedAt(job.getSubmittedAt())
                .build();
    }
}