package com.codesync.version.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Snapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commit_hash", nullable = false, unique = true, length = 64)
    private String commitHash;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "parent_hash", length = 64)
    private String parentHash;

    @Lob
    @Column(name = "full_content", nullable = false, columnDefinition = "LONGTEXT")
    private String fullContent;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "commit_message", length = 500)
    private String commitMessage;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "branch_id")
    private Long branchId;
}
