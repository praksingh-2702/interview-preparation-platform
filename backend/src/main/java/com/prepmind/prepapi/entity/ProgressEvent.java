package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Append-only activity log. Powers the heatmap and streaks. */
@Entity
@Table(name = "progress_events", indexes = {
    @Index(name = "idx_progress_events_user_time", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ProgressEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgressEventType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProgressEvent(User user, Question question, ProgressEventType type, Instant createdAt) {
        this.user = user;
        this.question = question;
        this.type = type;
        this.createdAt = createdAt;
    }
}
