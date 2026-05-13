package com.app.collabservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KickParticipantRequest {
    private Long ownerId;
    private Long participantUserId;
}