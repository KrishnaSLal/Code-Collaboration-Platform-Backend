package com.app.collabservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinSessionRequest {
    private Long userId;
    private String role;
    private String sessionPassword;
}