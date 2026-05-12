package com.app.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collab_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSession {

    @Id
    @Column(nullable = false, updatable = false)
    private String sessionId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String status;

    private String language;

    private LocalDateTime createdAt;

    private LocalDateTime endedAt;

    private Integer maxParticipants;

    private Boolean passwordProtected;

    private String sessionPassword;

    @PrePersist
    public void onCreate() {
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.passwordProtected == null) {
            this.passwordProtected = false;
        }
    }
}