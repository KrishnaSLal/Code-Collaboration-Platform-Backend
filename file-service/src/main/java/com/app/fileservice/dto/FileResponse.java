package com.app.fileservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private Long fileId;
    private Long projectId;
    private String name;
    private String path;
    private String language;
    private String content;
    private Long size;
    private Boolean folder;
    private Long createdById;
    private Long lastEditedBy;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}