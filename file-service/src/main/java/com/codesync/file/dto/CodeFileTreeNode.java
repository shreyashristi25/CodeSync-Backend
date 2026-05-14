package com.codesync.file.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFileTreeNode {
    private Long id;
    private String name;
    private String path;
    private Long parentId;
    private Long projectId;
    private String content;
    private Boolean isDirectory;
    private String language;
    private Long createdBy;
    private Long lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<CodeFileTreeNode> children = new ArrayList<>();
}
