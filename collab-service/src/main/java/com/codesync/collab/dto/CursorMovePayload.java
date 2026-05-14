package com.codesync.collab.dto;

import jakarta.validation.constraints.NotNull;

public class CursorMovePayload {
    @NotNull
    private Integer lineNumber;
    @NotNull
    private Integer column;
    @NotNull
    private String userId;

    public CursorMovePayload() {}

    public CursorMovePayload(Integer lineNumber, Integer column, String userId) {
        this.lineNumber = lineNumber;
        this.column = column;
        this.userId = userId;
    }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
    public Integer getColumn() { return column; }
    public void setColumn(Integer column) { this.column = column; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
