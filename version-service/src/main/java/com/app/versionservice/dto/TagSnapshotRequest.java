package com.app.versionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagSnapshotRequest {
    private String snapshotId;
    private String tag;
}