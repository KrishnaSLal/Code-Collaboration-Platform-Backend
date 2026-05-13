package com.app.notificationservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void onCreateInitializesDefaults() {
        Notification notification = Notification.builder()
                .type("INFO")
                .build();

        notification.onCreate();

        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getIsRead()).isFalse();
    }

    @Test
    void onCreatePreservesExistingValues() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        Notification notification = Notification.builder()
                .type("INFO")
                .createdAt(createdAt)
                .isRead(true)
                .build();

        notification.onCreate();

        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getIsRead()).isTrue();
    }
}
