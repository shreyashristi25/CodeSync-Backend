package com.codesync.version.dto;

import com.codesync.version.entity.Branch;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchResponse {
    private Long id;
    private String name;
    private Long projectId;
    private String latestSnapshotHash;
    private Boolean isDefault;
    private Instant createdAt;

    public static BranchResponse fromEntity(Branch b) {
        return BranchResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .projectId(b.getProjectId())
                .latestSnapshotHash(b.getLatestSnapshotHash())
                .isDefault(b.getIsDefault())
                .createdAt(b.getCreatedAt())
                .build();
    }
}