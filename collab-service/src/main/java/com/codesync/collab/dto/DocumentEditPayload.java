package com.codesync.collab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DocumentEditPayload {
    private String userId;
    
    @NotBlank
    private String operationType;
    
    @NotNull
    private Integer position;
    
    private Integer length;
    
    private String text;

    public DocumentEditPayload() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}