package com.app.projectservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private Long projectId;
    private Long ownerId;
    private String projectName;
    private String description;
    private String language;
    private String visibility;
    private Boolean archived;
    private Integer starCount;
    private Integer forkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}