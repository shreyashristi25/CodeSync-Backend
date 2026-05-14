package com.codesync.version.dto;

import com.codesync.version.entity.PullRequest;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PullRequestResponse {
    private Long id;
    private Long repositoryId;
    private String title;
    private String description;
    private Long sourceBranchId;
    private Long targetBranchId;
    private String status;
    private Long authorId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant mergedAt;

    public static PullRequestResponse fromEntity(PullRequest pr) {
        return PullRequestResponse.builder()
                .id(pr.getId())
                .repositoryId(pr.getRepositoryId())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .sourceBranchId(pr.getSourceBranchId())
                .targetBranchId(pr.getTargetBranchId())
                .status(pr.getStatus().name())
                .authorId(pr.getAuthorId())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .mergedAt(pr.getMergedAt())
                .build();
    }
}