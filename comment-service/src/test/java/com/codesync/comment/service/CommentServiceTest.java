package com.codesync.comment.service;

import com.codesync.comment.dto.CommentDTO;
import com.codesync.comment.dto.CreateCommentRequest;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.CommentNotFoundException;
import com.codesync.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;
    private CreateCommentRequest testRequest;

    @BeforeEach
    void setUp() {
        testComment = Comment.builder()
                .id(1L)
                .fileId(1L)
                .lineNumber(10)
                .content("Test comment")
                .resolved(false)
                .authorId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = CreateCommentRequest.builder()
                .fileId(1L)
                .lineNumber(10)
                .content("Test comment")
                .authorId(1L)
                .build();
    }

    @Test
    void testCreateComment_Success() {
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentDTO result = commentService.createComment(testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getFileId());
        assertEquals(10, result.getLineNumber());
        assertEquals("Test comment", result.getContent());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void testCreateComment_WithParentComment() {
        Comment parentComment = Comment.builder()
                .id(2L)
                .fileId(1L)
                .lineNumber(10)
                .content("Parent comment")
                .resolved(false)
                .authorId(1L)
                .build();

        testRequest.setParentCommentId(2L);
        testComment.setParentComment(parentComment);

        when(commentRepository.findById(2L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentDTO result = commentService.createComment(testRequest);

        assertNotNull(result);
        assertEquals(2L, result.getParentCommentId());
        verify(commentRepository, times(1)).findById(2L);
    }

    @Test
    void testGetComment_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        CommentDTO result = commentService.getComment(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(commentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetComment_NotFound() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.getComment(999L));
    }

    @Test
    void testGetCommentsByFileAndLine() {
        when(commentRepository.findByFileIdAndLineNumberOrderByCreatedAtAsc(1L, 10))
                .thenReturn(List.of(testComment));

        List<CommentDTO> result = commentService.getCommentsByFileAndLine(1L, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test comment", result.get(0).getContent());
    }

    @Test
    void testGetCommentsByFile() {
        when(commentRepository.findByFileIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testComment));

        List<CommentDTO> result = commentService.getCommentsByFile(1L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testResolveComment_Success() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentDTO result = commentService.resolveComment(1L);

        assertNotNull(result);
        assertTrue(result.getResolved());
    }

    @Test
    void testDeleteComment_Success() {
        when(commentRepository.findByIdAndAuthorId(1L, 1L)).thenReturn(Optional.of(testComment));

        commentService.deleteComment(1L, 1L);

        verify(commentRepository, times(1)).delete(testComment);
    }

    @Test
    void testDeleteComment_Unauthorized() {
        when(commentRepository.findByIdAndAuthorId(1L, 999L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(1L, 999L));
    }
}
