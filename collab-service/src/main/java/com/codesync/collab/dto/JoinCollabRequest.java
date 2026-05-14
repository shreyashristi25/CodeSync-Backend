package com.codesync.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinCollabRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String displayName;
    private Long projectId;
    private String sessionPassword;

    public JoinCollabRequest() {}

    public JoinCollabRequest(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getSessionPassword() { return sessionPassword; }
    public void setSessionPassword(String sessionPassword) { this.sessionPassword = sessionPassword; }
}
