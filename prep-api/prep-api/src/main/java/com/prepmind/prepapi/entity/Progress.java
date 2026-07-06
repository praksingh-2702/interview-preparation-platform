package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "question_id"})
})
@Data
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

    @Column(nullable = false)
    private boolean isSolved = false; 

    @Column(nullable = false)
    private boolean isRevised = false; 

    // 🚀 NEW COLUMN FIELD: Adds the missing status mapping column to satisfy database constraint
    @Column(nullable = false)
    private String status = "IN_PROGRESS"; 

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Explicit setters to align your DTO inputs cleanly
    public void setSolved(boolean solved) {
        this.isSolved = solved;
        updateStatusString(); // Keep status calculated automatically
    }

    public void setRevised(boolean revised) {
        this.isRevised = revised;
        updateStatusString(); // Keep status calculated automatically
    }

    // Helper method to automatically populate the database status string column dynamically
    private void updateStatusString() {
        if (this.isSolved) {
            this.status = "SOLVED";
        } else if (this.isRevised) {
            this.status = "REVISED";
        } else {
            this.status = "IN_PROGRESS";
        }
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        updateStatusString(); // Safety fallback check before executing database writing tasks
    }
}