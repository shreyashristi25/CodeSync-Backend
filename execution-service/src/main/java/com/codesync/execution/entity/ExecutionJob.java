package com.codesync.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "execution_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 48)
    private String jobId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExecutionLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    /** Optional stdin to pipe into the process. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stdin;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String output;

    /** Captured stderr stream (separate from stdout). */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stderr;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String error;

    @Column
    private Long executionTimeMs;

    @Column
    private Long memoryUsedKb;

    @Column
    private Long projectId;

    @Column(length = 512)
    private String fileName;

    @Column
    private java.time.Instant submittedAt;

    public static String newJobId() {
        return UUID.randomUUID().toString();
    }
}
