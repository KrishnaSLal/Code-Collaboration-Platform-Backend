package com.app.commentservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long commentId;
    private Long projectId;
    private Long fileId;
    private Long authorId;
    private String content;
    private Integer lineNumber;
    private Integer columnNumber;
    private Long parentCommentId;
    private Boolean resolved;
    private String snapshotId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}