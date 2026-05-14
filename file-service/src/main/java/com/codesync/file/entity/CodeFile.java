package com.codesync.file.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "code_files",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "branch_id", "path"}),
        indexes = {
            @Index(name = "idx_project_branch", columnList = "project_id, branch_id"),
            @Index(name = "idx_parent_id", columnList = "parent_id"),
            @Index(name = "idx_is_deleted", columnList = "is_deleted")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "branch_id")
    private Long branchId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String path;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private Boolean isDirectory;

    @Column(name = "is_deleted", nullable = false)
    @JsonProperty("isDeleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 50)
    private String language;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Long lastModifiedBy;
}
