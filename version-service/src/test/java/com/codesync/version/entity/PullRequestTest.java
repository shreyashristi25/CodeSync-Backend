package com.codesync.version.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PullRequestTest {

    @Test
    void shouldCreatePullRequest() {
        PullRequest pr = PullRequest.builder()
                .id(1L)
                .repositoryId(1L)
                .title("Test PR")
                .description("Description")
                .sourceBranchId(1L)
                .targetBranchId(2L)
                .authorId(1L)
                .status(PullRequest.PullRequestStatus.OPEN)
                .createdAt(Instant.now())
                .build();

        assertNotNull(pr);
        assertEquals("Test PR", pr.getTitle());
        assertEquals(PullRequest.PullRequestStatus.OPEN, pr.getStatus());
    }

    @Test
    void shouldUpdatePullRequestStatus() {
        PullRequest pr = PullRequest.builder()
                .id(1L)
                .status(PullRequest.PullRequestStatus.OPEN)
                .build();

        pr.setStatus(PullRequest.PullRequestStatus.MERGED);
        pr.setMergedAt(Instant.now());

        assertEquals(PullRequest.PullRequestStatus.MERGED, pr.getStatus());
        assertNotNull(pr.getMergedAt());
    }

    @Test
    void shouldUpdatePullRequestStatusToClosed() {
        PullRequest pr = PullRequest.builder()
                .id(1L)
                .status(PullRequest.PullRequestStatus.OPEN)
                .build();

        pr.setStatus(PullRequest.PullRequestStatus.CLOSED);
        pr.setUpdatedAt(Instant.now());

        assertEquals(PullRequest.PullRequestStatus.CLOSED, pr.getStatus());
    }

    @Test
    void shouldCheckPullRequestStatusValues() {
        assertEquals("OPEN", PullRequest.PullRequestStatus.OPEN.name());
        assertEquals("MERGED", PullRequest.PullRequestStatus.MERGED.name());
        assertEquals("CLOSED", PullRequest.PullRequestStatus.CLOSED.name());
    }
}