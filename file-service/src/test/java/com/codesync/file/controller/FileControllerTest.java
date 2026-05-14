package com.codesync.file.controller;

import com.codesync.file.dto.CodeFileTreeNode;
import com.codesync.file.dto.CreateCodeFileRequest;
import com.codesync.file.dto.UpdateCodeFileRequest;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    @Test
    void shouldCreateFile() {
        CreateCodeFileRequest request = new CreateCodeFileRequest(1L, "test.java", "src/test.java", false, 1L, "code", "java", 1L, 1L);
        CodeFile file = CodeFile.builder().id(1L).name("test.java").path("src/test.java").build();
        
        when(fileService.create(any(CreateCodeFileRequest.class))).thenReturn(file);

        ResponseEntity<CodeFile> result = fileController.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void shouldGetFile() {
        CodeFile file = CodeFile.builder().id(1L).name("test.java").content("code").build();
        
        when(fileService.get(1L)).thenReturn(file);

        ResponseEntity<CodeFile> result = fileController.get(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldUpdateFile() {
        UpdateCodeFileRequest request = new UpdateCodeFileRequest("new.java", "src/new.java", "new code", "java", 1L);
        CodeFile file = CodeFile.builder().id(1L).name("new.java").build();
        
        when(fileService.update(1L, request)).thenReturn(file);

        ResponseEntity<CodeFile> result = fileController.update(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldDeleteFile() {
        ResponseEntity<Void> result = fileController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void shouldGetTree() {
        List<CodeFileTreeNode> tree = List.of(CodeFileTreeNode.builder().id(1L).name("root").build());
        
        when(fileService.tree(1L, 1L)).thenReturn(tree);

        ResponseEntity<List<CodeFileTreeNode>> result = fileController.tree(1L, 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void shouldGetTreeWithNullBranchId() {
        List<CodeFileTreeNode> tree = List.of(CodeFileTreeNode.builder().id(1L).name("root").build());
        
        when(fileService.tree(1L, null)).thenReturn(tree);

        ResponseEntity<List<CodeFileTreeNode>> result = fileController.tree(1L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldListFiles() {
        List<CodeFile> files = List.of(CodeFile.builder().id(1L).name("test.java").build());
        
        when(fileService.listByProject(1L, 1L)).thenReturn(files);

        ResponseEntity<List<CodeFile>> result = fileController.list(1L, 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }
}