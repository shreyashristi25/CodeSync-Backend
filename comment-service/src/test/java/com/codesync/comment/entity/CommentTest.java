package com.codesync.comment.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    void shouldCreateComment() {
        Comment comment = Comment.builder()
                .id(1L)
                .fileId(100L)
            .authorId(50L)
                .content("This is a comment")
                .lineNumber(10)
                .createdAt(LocalDateTime.now())
                .build();

        assertNotNull(comment);
        assertEquals(100L, comment.getFileId());
        assertEquals(50L, comment.getAuthorId());
        assertEquals("This is a comment", comment.getContent());
        assertEquals(10, comment.getLineNumber());
    }

    @Test
    void shouldSetCommentFields() {
        Comment comment = new Comment();
        comment.setId(2L);
        comment.setFileId(200L);
        comment.setAuthorId(60L);
        comment.setContent("Another comment");
        comment.setLineNumber(20);

        assertEquals(2L, comment.getId());
        assertEquals("Another comment", comment.getContent());
        assertEquals(20, comment.getLineNumber());
    }
}