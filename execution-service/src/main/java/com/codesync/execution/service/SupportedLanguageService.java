package com.codesync.execution.service;

import com.codesync.execution.dto.SupportedLanguageDto;
import com.codesync.execution.dto.SupportedLanguageUpdateRequest;
import com.codesync.execution.entity.ExecutionLanguage;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.repository.SupportedLanguageRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupportedLanguageService {
    private final SupportedLanguageRepository supportedLanguageRepository;

    public List<SupportedLanguageDto> list() {
        seedDefaults();
        return supportedLanguageRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SupportedLanguageDto update(Long id, SupportedLanguageUpdateRequest request) {
        SupportedLanguage language = supportedLanguageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Language not found"));
        language.setDisplayName(request.displayName());
        language.setEnabled(request.enabled());
        if (request.dockerImage() != null) {
            language.setDockerImage(request.dockerImage());
        }
        if (request.entryPoint() != null) {
            language.setEntryPoint(request.entryPoint());
        }
        return toDto(supportedLanguageRepository.save(language));
    }

    private SupportedLanguageDto toDto(SupportedLanguage language) {
        return new SupportedLanguageDto(
                language.getId(), 
                language.getCode(), 
                language.getDisplayName(), 
                language.isEnabled(),
                language.getDockerImage(),
                language.getEntryPoint());
    }

    private void seedDefaults() {
        if (supportedLanguageRepository.count() > 0) {
            return;
        }
        
        java.util.Map<String, String[]> defaults = new java.util.HashMap<>();
        defaults.put("PYTHON", new String[]{"Python", "python:3.11-slim", "python3"});
        defaults.put("NODE", new String[]{"Node.js", "node:20-alpine", "node"});
        defaults.put("JAVA", new String[]{"Java", "openjdk:17-slim", "java"});
        defaults.put("C", new String[]{"C", "gcc:12-slim", "gcc"});
        defaults.put("CPP", new String[]{"C++", "gcc:12-slim", "g++"});
        defaults.put("GO", new String[]{"Go", "golang:1.21-alpine", "go"});
        defaults.put("RUST", new String[]{"Rust", "rust:1.75-slim", "rustc"});
        defaults.put("RUBY", new String[]{"Ruby", "ruby:3.3-slim", "ruby"});
        defaults.put("TYPESCRIPT", new String[]{"TypeScript", "node:20-alpine", "npx"});
        defaults.put("PHP", new String[]{"PHP", "php:8.3-cli", "php"});
        defaults.put("KOTLIN", new String[]{"Kotlin", "kotlin:1.9-slim", "kotlinc"});
        defaults.put("SWIFT", new String[]{"Swift", "swift:5.9-slim", "swift"});
        defaults.put("R", new String[]{"R", "r:4.3-slim", "Rscript"});

        Arrays.stream(ExecutionLanguage.values()).forEach(lang -> {
            String[] def = defaults.getOrDefault(lang.name(), new String[]{lang.name(), "ubuntu:22.04", lang.name().toLowerCase()});
            supportedLanguageRepository.save(SupportedLanguage.builder()
                    .code(lang.name())
                    .displayName(def[0])
                    .enabled(true)
                    .dockerImage(def[1])
                    .entryPoint(def[2])
                    .build());
        });
    }
}
