package com.app.fileservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveFileRequest {
    private String newPath;
}