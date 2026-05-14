package com.codesync.version.dto;

import com.codesync.version.entity.Tag;
import java.time.Instant;

public record TagResponse(
        Long id,
        String name,
        String commitHash,
        String description,
        Instant createdAt
) {
    public static TagResponse fromEntity(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getCommitHash(),
                tag.getDescription(),
                tag.getCreatedAt()
        );
    }
}