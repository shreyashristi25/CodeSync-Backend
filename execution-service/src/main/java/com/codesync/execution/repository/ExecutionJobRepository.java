package com.codesync.execution.repository;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.JobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionJobRepository extends JpaRepository<ExecutionJob, Long> {
    Optional<ExecutionJob> findByJobId(String jobId);
    List<ExecutionJob> findByStatus(JobStatus status);
    List<ExecutionJob> findAllByOrderBySubmittedAtDesc();
}
