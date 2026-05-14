package com.codesync.version.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    @Test
    void shouldCreateRepository() {
        Repository repo = Repository.builder()
                .id(1L)
                .name("test-repo")
                .projectId(1L)
                .defaultBranchId(1L)
                .isPublic(false)
                .createdAt(Instant.now())
                .build();

        assertNotNull(repo);
        assertEquals("test-repo", repo.getName());
        assertEquals(1L, repo.getProjectId());
        assertFalse(repo.getIsPublic());
    }

    @Test
    void shouldSetPublicRepository() {
        Repository repo = Repository.builder()
                .id(1L)
                .name("public-repo")
                .isPublic(false)
                .build();

        repo.setIsPublic(true);

        assertTrue(repo.getIsPublic());
    }

    @Test
    void shouldSetDefaultBranch() {
        Repository repo = Repository.builder()
                .id(1L)
                .defaultBranchId(null)
                .build();

        repo.setDefaultBranchId(5L);

        assertEquals(5L, repo.getDefaultBranchId());
    }

    @Test
    void shouldTrackCreatedAt() {
        Instant now = Instant.now();
        Repository repo = Repository.builder()
                .id(1L)
                .createdAt(now)
                .build();

        assertEquals(now, repo.getCreatedAt());
    }
}