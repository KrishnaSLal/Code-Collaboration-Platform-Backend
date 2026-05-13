package com.app.versionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreSnapshotRequest {
    private Long authorId;
    private String message;
}