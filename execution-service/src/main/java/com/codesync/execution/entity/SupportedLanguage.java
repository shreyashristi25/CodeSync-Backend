package com.codesync.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supported_languages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportedLanguage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 255)
    private String dockerImage;

    @Column(length = 64)
    private String entryPoint;
}
