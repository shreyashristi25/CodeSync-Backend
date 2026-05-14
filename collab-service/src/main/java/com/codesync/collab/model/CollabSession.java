package com.codesync.collab.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collab_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollabSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionUuid;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastActivityAt;

    @Column(length = 255)
    private String sessionPassword;

    @Column(name = "host_user_id", length = 128)
    private String hostUserId;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxParticipants = 10;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();
}
