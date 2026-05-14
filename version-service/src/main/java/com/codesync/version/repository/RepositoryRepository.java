package com.codesync.version.repository;

import com.codesync.version.entity.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    Optional<Repository> findByProjectIdAndName(Long projectId, String name);

    List<Repository> findByProjectId(Long projectId);
}