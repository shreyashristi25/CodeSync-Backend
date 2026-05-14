package com.codesync.version.dto;

import com.codesync.version.entity.Repository;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepositoryResponse {
    private Long id;
    private String name;
    private Long projectId;
    private Long defaultBranchId;
    private Boolean isPublic;
    private Instant createdAt;

    public static RepositoryResponse fromEntity(Repository r) {
        return RepositoryResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .projectId(r.getProjectId())
                .defaultBranchId(r.getDefaultBranchId())
                .isPublic(r.getIsPublic())
                .createdAt(r.getCreatedAt())
                .build();
    }
}