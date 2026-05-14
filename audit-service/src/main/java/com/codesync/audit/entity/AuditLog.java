package com.codesync.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_action", columnList = "user_id, action"),
    @Index(name = "idx_timestamp", columnList = "created_at DESC"),
    @Index(name = "idx_resource", columnList = "resource_type, resource_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String userEmail;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column(columnDefinition = "JSON")
    private String previousValue;

    @Column(columnDefinition = "JSON")
    private String newValue;

    @Column(length = 500)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static class Action {
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String PROJECT_CREATE = "PROJECT_CREATE";
        public static final String PROJECT_UPDATE = "PROJECT_UPDATE";
        public static final String PROJECT_DELETE = "PROJECT_DELETE";
        public static final String PROJECT_FORK = "PROJECT_FORK";
        public static final String PROJECT_FORCE_DELETE = "PROJECT_FORCE_DELETE";
        public static final String PROJECT_INVITE = "PROJECT_INVITE";
        public static final String PROJECT_INVITE_ACCEPT = "PROJECT_INVITE_ACCEPT";
        public static final String SESSION_START = "SESSION_START";
        public static final String SESSION_END = "SESSION_END";
        public static final String CODE_EXECUTE = "CODE_EXECUTE";
        public static final String LANGUAGE_UPDATE = "LANGUAGE_UPDATE";
        public static final String USER_ROLE_CHANGE = "USER_ROLE_CHANGE";
        public static final String USER_SUSPEND = "USER_SUSPEND";
    }
}