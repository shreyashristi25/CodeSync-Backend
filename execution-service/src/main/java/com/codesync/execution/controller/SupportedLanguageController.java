package com.codesync.execution.controller;

import com.codesync.execution.dto.SupportedLanguageDto;
import com.codesync.execution.dto.SupportedLanguageUpdateRequest;
import com.codesync.execution.service.SupportedLanguageService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/execution/admin/languages")
@RequiredArgsConstructor
public class SupportedLanguageController {
    private final SupportedLanguageService supportedLanguageService;

    @GetMapping
    public ResponseEntity<List<SupportedLanguageDto>> list() {
        return ResponseEntity.ok(supportedLanguageService.list());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupportedLanguageDto> update(
            @PathVariable Long id,
            @Valid @RequestBody SupportedLanguageUpdateRequest request) {
        return ResponseEntity.ok(supportedLanguageService.update(id, request));
    }
}
