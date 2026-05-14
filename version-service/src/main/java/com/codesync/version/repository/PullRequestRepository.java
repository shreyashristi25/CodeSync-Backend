package com.codesync.version.repository;

import com.codesync.version.entity.PullRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    List<PullRequest> findByRepositoryId(Long repositoryId);

    List<PullRequest> findByRepositoryIdAndStatus(Long repositoryId, PullRequest.PullRequestStatus status);

    List<PullRequest> findByAuthorId(Long authorId);

    Optional<PullRequest> findByIdAndAuthorId(Long id, Long authorId);
}