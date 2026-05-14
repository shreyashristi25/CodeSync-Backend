package com.codesync.comment.controller;

import com.codesync.comment.dto.CommentDTO;
import com.codesync.comment.dto.CreateCommentRequest;
import com.codesync.comment.dto.UpdateCommentRequest;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    @Test
    void shouldCreateComment() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .fileId(1L).lineNumber(10).content("Test comment").authorId(1L).build();
        CommentDTO dto = CommentDTO.builder().id(1L).fileId(1L).lineNumber(10).content("Test comment").build();
        
        when(commentService.createComment(any(CreateCommentRequest.class))).thenReturn(dto);

        ResponseEntity<CommentDTO> result = commentController.createComment(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
    }

    @Test
    void shouldGetComment() {
        CommentDTO dto = CommentDTO.builder().id(1L).fileId(1L).content("Test").build();
        
        when(commentService.getComment(1L)).thenReturn(dto);

        ResponseEntity<CommentDTO> result = commentController.getComment(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldGetCommentsByFile() {
        List<CommentDTO> comments = List.of(CommentDTO.builder().id(1L).fileId(1L).build());
        
        when(commentService.getCommentsByFile(1L)).thenReturn(comments);

        ResponseEntity<List<CommentDTO>> result = commentController.getCommentsByFile(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void shouldGetCommentsByFileAndLine() {
        List<CommentDTO> comments = List.of(CommentDTO.builder().id(1L).fileId(1L).lineNumber(10).build());
        
        when(commentService.getCommentsByFileAndLine(1L, 10)).thenReturn(comments);

        ResponseEntity<List<CommentDTO>> result = commentController.getCommentsByFileAndLine(1L, 10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void shouldResolveComment() {
        CommentDTO dto = CommentDTO.builder().id(1L).resolved(true).build();
        
        when(commentService.resolveComment(1L)).thenReturn(dto);

        ResponseEntity<CommentDTO> result = commentController.resolveComment(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().getResolved());
    }

    @Test
    void shouldDeleteComment() {
        ResponseEntity<Void> result = commentController.deleteComment(1L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void shouldUpdateComment() {
        CommentDTO dto = CommentDTO.builder().id(1L).content("Updated").build();
        
        when(commentService.updateComment(1L, "Updated", 1L)).thenReturn(dto);

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated");
        request.setAuthorId(1L);
        ResponseEntity<CommentDTO> result = commentController.updateComment(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}