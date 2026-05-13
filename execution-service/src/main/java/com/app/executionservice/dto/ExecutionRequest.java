package com.app.executionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRequest {
    private Long projectId;
    private Long fileId;
    private Long userId;
    private String language;
    private String sourceCode;
    private String stdin;
}