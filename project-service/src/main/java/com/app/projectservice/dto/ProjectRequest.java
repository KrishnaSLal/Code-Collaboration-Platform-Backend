package com.app.projectservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    private Long ownerId;
    private String projectName;
    private String description;
    private String language;
    private String visibility;
}