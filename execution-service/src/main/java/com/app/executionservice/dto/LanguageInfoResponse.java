package com.app.executionservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageInfoResponse {
    private String language;
    private String version;
}