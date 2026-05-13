package com.app.executionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionStatsResponse {
    private Long totalExecutions;
    private Long completedExecutions;
    private Long failedExecutions;
    private Long cancelledExecutions;
}