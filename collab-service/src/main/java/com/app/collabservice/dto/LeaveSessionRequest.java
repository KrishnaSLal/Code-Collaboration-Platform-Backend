package com.app.collabservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveSessionRequest {
    private Long userId;
}