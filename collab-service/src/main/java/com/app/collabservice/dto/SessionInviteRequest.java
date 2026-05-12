package com.app.collabservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInviteRequest {
    @NotNull
    private Long recipientId;
    @NotNull
    private Long actorId;
    private String title;
    private String message;
}
