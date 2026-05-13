package com.app.fileservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRequest {
    private Long projectId;
    private String name;
    private String path;
    private String language;
    private String content;
    private Long createdById;
}