package com.codesync.collab.dto;

import jakarta.validation.constraints.NotBlank;

public class LeaveCollabRequest {
    @NotBlank
    private String userId;

    public LeaveCollabRequest() {}

    public LeaveCollabRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
