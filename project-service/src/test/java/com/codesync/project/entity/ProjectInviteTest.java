package com.codesync.project.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ProjectInviteTest {

    @Test
    void shouldCreateProjectInvite() {
        ProjectInvite invite = ProjectInvite.builder()
                .id(1L)
                .projectId(100L)
                .invitedByUserId(10L)
                .userId(20L)
                .inviteToken("abc123token")
                .status(ProjectInvite.InviteStatus.PENDING)
                .build();

        assertNotNull(invite);
        assertEquals(100L, invite.getProjectId());
        assertEquals(20L, invite.getUserId());
        assertEquals("abc123token", invite.getInviteToken());
    }

    @Test
    void shouldSetProjectInviteFields() {
        ProjectInvite invite = new ProjectInvite();
        invite.setId(2L);
        invite.setProjectId(200L);
        invite.setInvitedByUserId(50L);
        invite.setUserId(60L);
        invite.setInviteToken("xyz789token");

        assertEquals(2L, invite.getId());
        assertEquals(200L, invite.getProjectId());
        assertEquals("xyz789token", invite.getInviteToken());
    }
}