package com.codesync.file.controller;

import com.codesync.file.dto.CodeFileTreeNode;
import com.codesync.file.dto.CreateCodeFileRequest;
import com.codesync.file.dto.UpdateCodeFileRequest;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.service.FileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping
    public ResponseEntity<CodeFile> create(@Valid @RequestBody CreateCodeFileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodeFile> get(@PathVariable Long id) {
        return ResponseEntity.ok(fileService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CodeFile> update(@PathVariable Long id, @RequestBody UpdateCodeFileRequest request) {
        return ResponseEntity.ok(fileService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fileService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CodeFileTreeNode>> tree(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(fileService.tree(projectId, branchId));
    }

    @GetMapping
    public ResponseEntity<List<CodeFile>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(fileService.listByProject(projectId, branchId));
    }
}
