package com.app.versionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSnapshotRequest {
    private Long projectId;
    private Long fileId;
    private Long authorId;
    private String message;
    private String content;
    private String parentSnapshotId;
    private String branch;
}