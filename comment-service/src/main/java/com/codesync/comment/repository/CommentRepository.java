package com.codesync.comment.repository;

import com.codesync.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByFileIdAndLineNumberOrderByCreatedAtAsc(Long fileId, Integer lineNumber);
    List<Comment> findByFileIdOrderByCreatedAtDesc(Long fileId);
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
    Optional<Comment> findByIdAndAuthorId(Long commentId, Long authorId);
}
