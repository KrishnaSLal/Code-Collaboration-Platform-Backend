package com.app.commentservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCountResponse {
    private Long fileId;
    private Long count;
}