package com.codesync.version.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SnapshotTest {

    @Test
    void shouldCreateSnapshot() {
        Snapshot snapshot = Snapshot.builder()
                .id(1L)
                .fileId(100L)
                .fullContent("file content here")
                .commitMessage("Initial commit")
            .timestamp(Instant.now())
                .build();

        assertNotNull(snapshot);
        assertEquals(100L, snapshot.getFileId());
        assertEquals("Initial commit", snapshot.getCommitMessage());
    }

    @Test
    void shouldSetSnapshotFields() {
        Snapshot snapshot = new Snapshot();
        snapshot.setId(2L);
        snapshot.setFileId(200L);
        snapshot.setFullContent("updated content");
        snapshot.setCommitMessage("Update 1");

        assertEquals(2L, snapshot.getId());
        assertEquals("updated content", snapshot.getFullContent());
    }
}

class BranchTest {

    @Test
    void shouldCreateBranch() {
        Branch branch = Branch.builder()
                .id(1L)
            .projectId(100L)
                .name("main")
                .isDefault(true)
            .createdAt(Instant.now())
                .build();

        assertNotNull(branch);
        assertEquals("main", branch.getName());
        assertEquals(true, branch.getIsDefault());
    }

    @Test
    void shouldSetBranchFields() {
        Branch branch = new Branch();
        branch.setId(2L);
        branch.setProjectId(200L);
        branch.setName("feature-branch");
        branch.setIsDefault(false);
        branch.setCreatedAt(Instant.now());

        assertEquals("feature-branch", branch.getName());
        assertEquals(false, branch.getIsDefault());
    }
}

class TagTest {

    @Test
    void shouldCreateTag() {
        Tag tag = Tag.builder()
                .id(1L)
                .name("v1.0.0")
                .commitHash("abc123")
            .createdAt(Instant.now())
                .build();

        assertNotNull(tag);
        assertEquals("v1.0.0", tag.getName());
        assertEquals("abc123", tag.getCommitHash());
    }
}