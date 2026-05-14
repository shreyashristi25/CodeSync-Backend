package com.codesync.version.dto;

import com.codesync.version.entity.Snapshot;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SnapshotSummaryResponse {
    private String commitHash;
    private Long fileId;
    private String parentHash;
    private Instant timestamp;
    private String commitMessage;
    private Long authorId;
    private Long branchId;

    public static SnapshotSummaryResponse fromEntity(Snapshot s) {
        return SnapshotSummaryResponse.builder()
                .commitHash(s.getCommitHash())
                .fileId(s.getFileId())
                .parentHash(s.getParentHash())
                .timestamp(s.getTimestamp())
                .commitMessage(s.getCommitMessage())
                .authorId(s.getAuthorId())
                .branchId(s.getBranchId())
                .build();
    }
}
