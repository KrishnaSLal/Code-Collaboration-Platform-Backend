package com.app.versionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiffResponse {
    private String snapshotIdOne;
    private String snapshotIdTwo;
    private String diffResult;
}