package com.codesync.file.dto;

public class UpdateCodeFileRequest {
    private String name;
    private String path;
    private String content;
    private String language;
    private Long lastModifiedBy;

    public UpdateCodeFileRequest() {}

    public UpdateCodeFileRequest(String name, String path, String content, String language, Long lastModifiedBy) {
        this.name = name;
        this.path = path;
        this.content = content;
        this.language = language;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Long getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(Long lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}
