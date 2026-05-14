package com.codesync.file.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesync.file.dto.CodeFileTreeNode;
import com.codesync.file.dto.CreateCodeFileRequest;
import com.codesync.file.dto.UpdateCodeFileRequest;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.BadRequestException;
import com.codesync.file.exception.NotFoundException;
import com.codesync.file.repository.CodeFileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private CodeFileRepository codeFileRepository;

    @InjectMocks
    private FileService fileService;

    @Test
    void shouldCreateFile() {
        when(codeFileRepository.save(any(CodeFile.class))).thenAnswer(invocation -> {
            CodeFile f = invocation.getArgument(0);
            f.setId(9L);
            return f;
        });
        CodeFile created =
                fileService.create(new CreateCodeFileRequest(1L, "a.ts", "src/a.ts", false, null, "x", "typescript", 1L, 1L));
        assertEquals(9L, created.getId());
        assertEquals("src/a.ts", created.getPath());
        assertFalse(created.getDeleted());
    }

    @Test
    void shouldCreateFileWhenNoDuplicate() {
        when(codeFileRepository.save(any(CodeFile.class))).thenAnswer(invocation -> {
            CodeFile f = invocation.getArgument(0);
            f.setId(9L);
            return f;
        });
        CodeFile created =
                fileService.create(new CreateCodeFileRequest(1L, "a.ts", "src/a.ts", false, null, "x", "typescript", 1L, 1L));
        assertEquals(9L, created.getId());
        assertEquals("src/a.ts", created.getPath());
        assertFalse(created.getDeleted());
    }

    @Test
    void shouldGetFile() {
        CodeFile f = CodeFile.builder().id(1L).path("p").content("c").deleted(false).projectId(1L).build();
        when(codeFileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(f));
        assertEquals("p", fileService.get(1L).getPath());
    }

    @Test
    void shouldThrowWhenMissing() {
        when(codeFileRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> fileService.get(2L));
    }

    @Test
    void shouldUpdateFile() {
        CodeFile f = CodeFile.builder().id(1L).name("old").path("old").content("a").deleted(false).projectId(1L).branchId(1L).lastModifiedBy(1L).build();
        when(codeFileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(f));
        when(codeFileRepository.existsByProjectIdAndBranchIdAndPathAndDeletedFalse(1L, 1L, "new")).thenReturn(false);
        when(codeFileRepository.save(any(CodeFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CodeFile updated = fileService.update(1L, new UpdateCodeFileRequest("new", "new", "b", "typescript", 1L));
        assertEquals("new", updated.getPath());
        assertEquals("b", updated.getContent());
    }

    @Test
    void shouldSoftDelete() {
        CodeFile f = CodeFile.builder().id(1L).path("p").content("").deleted(false).projectId(1L).build();
        when(codeFileRepository.findById(1L)).thenReturn(Optional.of(f));
        when(codeFileRepository.save(any(CodeFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        fileService.softDelete(1L);
        assertTrue(f.getDeleted());
    }

    @Test
    void shouldBuildTree() {
        CodeFile root = CodeFile.builder().id(1L).path("r").parentId(null).content("").deleted(false).projectId(1L).branchId(1L).build();
        CodeFile child = CodeFile.builder().id(2L).path("r/c").parentId(1L).content("").deleted(false).projectId(1L).branchId(1L).build();
        when(codeFileRepository.findByProjectIdAndBranchIdAndDeletedFalse(1L, 1L)).thenReturn(List.of(root, child));
        List<CodeFileTreeNode> tree = fileService.tree(1L, 1L);
        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getId());
    }

    @Test
    void shouldListByProject() {
        when(codeFileRepository.findByProjectIdAndBranchIdAndDeletedFalse(3L, 1L)).thenReturn(List.of());
        assertTrue(fileService.listByProject(3L, 1L).isEmpty());
        verify(codeFileRepository).findByProjectIdAndBranchIdAndDeletedFalse(3L, 1L);
        verify(codeFileRepository, never()).findById(anyLong());
        verify(codeFileRepository, never()).existsByProjectIdAndBranchIdAndPathAndDeletedFalse(anyLong(), anyLong(), anyString());
    }
}
