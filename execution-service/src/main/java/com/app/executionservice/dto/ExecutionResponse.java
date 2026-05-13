package com.app.executionservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResponse {
    private String jobId;
    private Long projectId;
    private Long fileId;
    private Long userId;
    private String language;
    private String sourceCode;
    private String stdin;
    private String status;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}