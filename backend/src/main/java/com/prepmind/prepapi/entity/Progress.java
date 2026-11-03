package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
