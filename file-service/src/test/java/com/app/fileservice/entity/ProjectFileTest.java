package com.app.fileservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFileTest {

    @Test
    void onCreateInitializesDefaultsAndTimestamps() {
        ProjectFile file = ProjectFile.builder()
                .projectId(1L)
                .name("Main.java")
                .path("src/Main.java")
                .build();

        file.onCreate();

        assertThat(file.getCreatedAt()).isNotNull();
        assertThat(file.getUpdatedAt()).isNotNull();
        assertThat(file.getDeleted()).isFalse();
        assertThat(file.getFolder()).isFalse();
        assertThat(file.getContent()).isEmpty();
        assertThat(file.getSize()).isZero();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        ProjectFile file = ProjectFile.builder()
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        file.onUpdate();

        assertThat(file.getUpdatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
