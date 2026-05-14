package com.codesync.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {
    @NotNull(message = "File ID cannot be null")
    private Long fileId;

    @NotNull(message = "Line number cannot be null")
    private Integer lineNumber;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    private Long authorId;

    private Long parentCommentId;
}
