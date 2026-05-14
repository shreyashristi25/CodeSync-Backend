package com.codesync.collab.model;

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
@Table(name = "collab_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollabHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant endedAt;

    @Column(name = "started_by_user_id", nullable = false, length = 128)
    private String startedByUserId;

    @Column(name = "ended_by_user_id", nullable = false, length = 128)
    private String endedByUserId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String participants;

    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;
}