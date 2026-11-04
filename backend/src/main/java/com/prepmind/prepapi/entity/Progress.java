package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "question_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // Column names match the schema that earlier versions created (is_solved / is_revised).
    @Column(name = "is_solved", nullable = false)
    private boolean solved;

    @Column(name = "is_revised", nullable = false)
    private boolean revised;

    /** Derived from the two flags; stored because the existing column is NOT NULL. */
    @Column(nullable = false)
    private String status = "IN_PROGRESS";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Revision schedule (v2). All nullable so existing rows migrate cleanly. ----

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    /** When this problem is next due in the revision queue. Null while unsolved. */
    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    /** Index into RevisionSchedule.INTERVAL_DAYS. Null while unsolved. */
    @Column(name = "review_stage")
    private Integer reviewStage;

    public void setSolved(boolean solved) {
        this.solved = solved;
        refreshDerivedFields();
    }

    public void setRevised(boolean revised) {
        this.revised = revised;
        refreshDerivedFields();
    }

    @PrePersist
    @PreUpdate
    protected void refreshDerivedFields() {
        updatedAt = LocalDateTime.now();
        status = solved ? "SOLVED" : revised ? "REVISED" : "IN_PROGRESS";
    }
}
