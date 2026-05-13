package com.app.versionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {
    private String snapshotId;
    private String branchName;
}