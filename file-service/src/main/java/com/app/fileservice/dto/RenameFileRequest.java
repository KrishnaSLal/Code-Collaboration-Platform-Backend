package com.app.fileservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenameFileRequest {
    private String newName;
}