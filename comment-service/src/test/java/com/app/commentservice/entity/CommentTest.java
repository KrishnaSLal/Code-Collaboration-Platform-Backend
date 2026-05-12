package com.app.commentservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    void onCreateInitializesMissingTimestampsAndResolvedFlag() {
        Comment comment = Comment.builder()
                .content("Looks good")
                .build();

        comment.onCreate();

        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNotNull();
        assertThat(comment.getResolved()).isFalse();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Comment comment = Comment.builder()
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        comment.onUpdate();

        assertThat(comment.getUpdatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
