package com.codesync.version.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pull_requests", indexes = {
    @Index(name = "idx_pr_repo", columnList = "repository_id"),
    @Index(name = "idx_pr_author", columnList = "author_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "source_branch_id", nullable = false)
    private Long sourceBranchId;

    @Column(name = "target_branch_id", nullable = false)
    private Long targetBranchId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PullRequestStatus status = PullRequestStatus.OPEN;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    public enum PullRequestStatus {
        OPEN, MERGED, CLOSED
    }
}