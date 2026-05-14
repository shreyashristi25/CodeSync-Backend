package com.codesync.comment.controller;

import com.codesync.comment.dto.CommentDTO;
import com.codesync.comment.dto.CreateCommentRequest;
import com.codesync.comment.dto.UpdateCommentRequest;
import com.codesync.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CreateCommentRequest request) {
        return new ResponseEntity<>(commentService.createComment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentDTO> getComment(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getComment(id));
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(commentService.getCommentsByFile(fileId));
    }

    @GetMapping("/file/{fileId}/line/{lineNumber}")
    public ResponseEntity<List<CommentDTO>> getCommentsByFileAndLine(
            @PathVariable Long fileId,
            @PathVariable Integer lineNumber) {
        return ResponseEntity.ok(commentService.getCommentsByFileAndLine(fileId, lineNumber));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<CommentDTO> resolveComment(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.resolveComment(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @RequestParam Long authorId) {
        commentService.deleteComment(id, authorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(id, request.getContent(), request.getAuthorId()));
    }
}
