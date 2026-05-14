package com.codesync.version.repository;

import com.codesync.version.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByProjectIdAndName(Long projectId, String name);

    List<Branch> findByProjectId(Long projectId);

    Optional<Branch> findByProjectIdAndIsDefaultTrue(Long projectId);
}