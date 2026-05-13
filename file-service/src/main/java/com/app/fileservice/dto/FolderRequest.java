package com.app.fileservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderRequest {
    private Long projectId;
    private String name;
    private String path;
    private Long createdById;
}