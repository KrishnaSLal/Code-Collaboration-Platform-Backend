package com.app.versionservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotTest {

    @Test
    void onCreateInitializesDefaults() {
        Snapshot snapshot = Snapshot.builder()
                .content("content")
                .build();

        snapshot.onCreate();

        assertThat(snapshot.getSnapshotId()).isNotBlank();
        assertThat(snapshot.getCreatedAt()).isNotNull();
        assertThat(snapshot.getBranch()).isEqualTo("main");
    }

    @Test
    void onCreatePreservesExistingValues() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        Snapshot snapshot = Snapshot.builder()
                .snapshotId("snap-1")
                .createdAt(createdAt)
                .branch("feature")
                .build();

        snapshot.onCreate();

        assertThat(snapshot.getSnapshotId()).isEqualTo("snap-1");
        assertThat(snapshot.getCreatedAt()).isEqualTo(createdAt);
        assertThat(snapshot.getBranch()).isEqualTo("feature");
    }
}
