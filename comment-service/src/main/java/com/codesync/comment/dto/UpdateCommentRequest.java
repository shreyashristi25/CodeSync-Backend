package com.codesync.comment.dto;

import lombok.Data;

@Data
public class UpdateCommentRequest {
    private String content;
    private Long authorId;
}