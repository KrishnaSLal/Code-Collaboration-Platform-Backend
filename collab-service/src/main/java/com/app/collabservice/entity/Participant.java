package com.app.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String role; 

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private Integer cursorLine;

    private Integer cursorCol;

    private String color;

    @PrePersist
    public void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.cursorLine == null) {
            this.cursorLine = 1;
        }
        if (this.cursorCol == null) {
            this.cursorCol = 1;
        }
    }
}