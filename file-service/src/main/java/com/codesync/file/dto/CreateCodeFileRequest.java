package com.codesync.file.dto;

public class CreateCodeFileRequest {
    private Long projectId;
    private String name;
    private String path;
    private Boolean isDirectory;
    private Long parentId;
    private String content;
    private String language;
    private Long createdBy;
    private Long branchId;

    public CreateCodeFileRequest() {}

    public CreateCodeFileRequest(Long projectId, String name, String path, Boolean isDirectory, Long parentId, String content, String language, Long createdBy, Long branchId) {
        this.projectId = projectId;
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.parentId = parentId;
        this.content = content;
        this.language = language;
        this.createdBy = createdBy;
        this.branchId = branchId;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Boolean getIsDirectory() { return isDirectory; }
    public void setIsDirectory(Boolean isDirectory) { this.isDirectory = isDirectory; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
}
