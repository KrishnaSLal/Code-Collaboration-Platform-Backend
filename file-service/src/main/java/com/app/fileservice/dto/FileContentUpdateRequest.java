package com.app.fileservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileContentUpdateRequest {
    private String content;
    private Long lastEditedBy;
}