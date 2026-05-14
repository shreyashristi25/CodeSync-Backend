package com.codesync.execution.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ExecutionJobTest {

    @Test
    void shouldCreateExecutionJobWithBuilder() {
        ExecutionJob job = ExecutionJob.builder()
                .id(1L)
                .jobId("test-job-123")
                .code("print('hello')")
                .language(ExecutionLanguage.PYTHON)
                .status(JobStatus.QUEUED)
                .stdin("")
                .output("hello\n")
                .stderr("")
                .error(null)
                .executionTimeMs(100L)
                .memoryUsedKb(500L)
                .build();

        assertNotNull(job);
        assertEquals("test-job-123", job.getJobId());
        assertEquals(ExecutionLanguage.PYTHON, job.getLanguage());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertEquals("hello\n", job.getOutput());
        assertEquals(100L, job.getExecutionTimeMs());
    }

    @Test
    void shouldSetJobFields() {
        ExecutionJob job = new ExecutionJob();
        job.setId(2L);
        job.setJobId("job-456");
        job.setCode("System.out.println(1);");
        job.setLanguage(ExecutionLanguage.JAVA);
        job.setStatus(JobStatus.COMPLETED);
        job.setOutput("1");

        assertEquals(2L, job.getId());
        assertEquals("job-456", job.getJobId());
        assertEquals(ExecutionLanguage.JAVA, job.getLanguage());
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void shouldCheckNewJobIdGeneration() {
        String jobId = ExecutionJob.newJobId();
        assertNotNull(jobId);
        assertEquals(36, jobId.length()); // UUID format
    }
}