package com.app.projectservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceProjectTest {

    @Test
    void onCreateSetsTimestamps() {
        WorkspaceProject project = WorkspaceProject.builder()
                .ownerId(1L)
                .projectName("CodeSync")
                .language("Java")
                .visibility("PRIVATE")
                .build();

        project.onCreate();

        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        WorkspaceProject project = WorkspaceProject.builder()
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        project.onUpdate();

        assertThat(project.getUpdatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
