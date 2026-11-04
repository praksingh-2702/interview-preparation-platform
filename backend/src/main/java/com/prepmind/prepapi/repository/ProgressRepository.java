package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.dto.ProgressView;
import com.prepmind.prepapi.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT new com.prepmind.prepapi.dto.ProgressView(p.question.id, p.solved, p.revised, "
            + "p.solvedAt, p.nextReviewAt, p.reviewStage) FROM Progress p WHERE p.user.id = :userId")
    List<ProgressView> findViewsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Progress p WHERE p.user.id = :userId AND p.solved = true "
            + "AND p.nextReviewAt IS NOT NULL AND p.nextReviewAt <= :now")
    long countDue(@Param("userId") Long userId, @Param("now") Instant now);

    /** Rows created before v2 that have no revision schedule yet. */
    List<Progress> findBySolvedTrueAndNextReviewAtIsNull();
}
