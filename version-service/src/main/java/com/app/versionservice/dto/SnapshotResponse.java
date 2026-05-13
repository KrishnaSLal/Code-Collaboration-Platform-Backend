package com.app.versionservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotResponse {
    private String snapshotId;
    private Long projectId;
    private Long fileId;
    private Long authorId;
    private String message;
    private String content;
    private String hash;
    private String parentSnapshotId;
    private String branch;
    private String tag;
    private LocalDateTime createdAt;
}