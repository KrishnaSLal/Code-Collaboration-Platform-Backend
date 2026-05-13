package com.app.commentservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCommentRequest {
    private Long projectId;
    private Long fileId;
    private Long authorId;
    private String content;
    private Integer lineNumber;
    private Integer columnNumber;
    private Long parentCommentId;
    private String snapshotId;
}