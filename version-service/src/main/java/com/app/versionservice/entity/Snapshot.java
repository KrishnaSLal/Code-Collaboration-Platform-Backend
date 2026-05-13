package com.app.versionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snapshot {

    @Id
    @Column(nullable = false, updatable = false)
    private String snapshotId;

    private Long projectId;

    private Long fileId;

    private Long authorId;

    private String message;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String hash;

    private String parentSnapshotId;

    private String branch;

    private String tag;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.snapshotId == null) {
            this.snapshotId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.branch == null) {
            this.branch = "main";
        }
    }
}