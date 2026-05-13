package com.app.notificationservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountResponse {
    private Long recipientId;
    private Long unreadCount;
}