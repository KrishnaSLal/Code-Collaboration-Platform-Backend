package com.app.executionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionJob {

    @Id
    @Column(nullable = false, updatable = false)
    private String jobId;

    private Long projectId;

    private Long fileId;

    private Long userId;

    @Column(nullable = false)
    private String language;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stdin;

    @Column(nullable = false)
    private String status; // QUEUED / RUNNING / COMPLETED / FAILED / TIMED_OUT / CANCELLED

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stdout;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String stderr;

    private Integer exitCode;

    private Long executionTimeMs;

    private Long memoryUsedKb;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        if (this.jobId == null) {
            this.jobId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = "QUEUED";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}