package com.app.notificationservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {
    private String to;
    private String subject;
    private String body;
}