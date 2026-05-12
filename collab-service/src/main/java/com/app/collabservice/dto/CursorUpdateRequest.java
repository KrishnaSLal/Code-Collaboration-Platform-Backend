package com.app.collabservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorUpdateRequest {
    private Long userId;
    private Integer cursorLine;
    private Integer cursorCol;
}