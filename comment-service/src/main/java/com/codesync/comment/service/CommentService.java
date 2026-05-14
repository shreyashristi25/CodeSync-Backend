package com.codesync.comment.service;

import com.codesync.comment.dto.CommentDTO;
import com.codesync.comment.dto.CreateCommentRequest;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.CommentNotFoundException;
import com.codesync.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentDTO createComment(CreateCommentRequest request) {
        log.info("Creating comment for file {} at line {}", request.getFileId(), request.getLineNumber());
        
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new CommentNotFoundException(
                            "Parent comment not found with ID: " + request.getParentCommentId()));
        }

        Comment comment = Comment.builder()
                .fileId(request.getFileId())
                .lineNumber(request.getLineNumber())
                .content(request.getContent())
                .authorId(request.getAuthorId())
                .parentComment(parentComment)
                .resolved(false)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment created with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    public CommentDTO getComment(Long id) {
        log.info("Fetching comment with ID: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with ID: " + id));
        return mapToDTO(comment);
    }

    public List<CommentDTO> getCommentsByFileAndLine(Long fileId, Integer lineNumber) {
        log.info("Fetching comments for file {} at line {}", fileId, lineNumber);
        List<Comment> comments = commentRepository.findByFileIdAndLineNumberOrderByCreatedAtAsc(fileId, lineNumber);
        return comments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CommentDTO> getCommentsByFile(Long fileId) {
        log.info("Fetching all comments for file {}", fileId);
        List<Comment> comments = commentRepository.findByFileIdOrderByCreatedAtDesc(fileId);
        return comments.stream()
                .filter(c -> c.getParentComment() == null)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CommentDTO resolveComment(Long id) {
        log.info("Resolving comment with ID: {}", id);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with ID: " + id));
        comment.setResolved(!comment.getResolved());
        Comment updated = commentRepository.save(comment);
        log.info("Comment {} resolved status set to {}", id, updated.getResolved());
        return mapToDTO(updated);
    }

    public void deleteComment(Long id, Long authorId) {
        log.info("Deleting comment with ID: {} by author {}", id, authorId);
        Comment comment = commentRepository.findByIdAndAuthorId(id, authorId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found or unauthorized"));
        commentRepository.delete(comment);
        log.info("Comment {} deleted", id);
    }

    public CommentDTO updateComment(Long id, String newContent, Long authorId) {
        log.info("Updating comment with ID: {}", id);
        Comment comment = commentRepository.findByIdAndAuthorId(id, authorId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found or unauthorized"));
        comment.setContent(newContent);
        Comment updated = commentRepository.save(comment);
        log.info("Comment {} updated", id);
        return mapToDTO(updated);
    }

    private CommentDTO mapToDTO(Comment comment) {
        List<CommentDTO> replies = comment.getReplies() != null 
                ? comment.getReplies().stream().map(this::mapToDTO).collect(Collectors.toList())
                : List.of();
        
        return CommentDTO.builder()
                .id(comment.getId())
                .fileId(comment.getFileId())
                .lineNumber(comment.getLineNumber())
                .content(comment.getContent())
                .resolved(comment.getResolved())
                .authorId(comment.getAuthorId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }
}
