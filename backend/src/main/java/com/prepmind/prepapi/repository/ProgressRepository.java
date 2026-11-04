package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.dto.ProgressView;
import com.prepmind.prepapi.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** When each currently ticked (Solved / Learned) row was ticked: the heatmap's data, all sections together. */
    @Query("SELECT p.solvedAt FROM Progress p WHERE p.user.id = :userId AND p.solved = true AND p.solvedAt IS NOT NULL")
    List<Instant> findSolvedTimestampsByUserId(@Param("userId") Long userId);

    /** [userId, difficulty, count] for every ticked row: leaderboard points and admin stats. */
    @Query("SELECT p.user.id, p.question.difficulty, COUNT(p) FROM Progress p WHERE p.solved = true "
            + "GROUP BY p.user.id, p.question.difficulty")
    List<Object[]> countSolvedByUserAndDifficulty();

    /** [userId, solvedAt] for every ticked row: leaderboard streaks. */
    @Query("SELECT p.user.id, p.solvedAt FROM Progress p WHERE p.solved = true AND p.solvedAt IS NOT NULL")
    List<Object[]> findAllSolvedTimestamps();

    @Modifying
    @Query("DELETE FROM Progress p WHERE p.question.id = :questionId")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /** Rows created before v2 that have no revision schedule yet. */
    List<Progress> findBySolvedTrueAndNextReviewAtIsNull();
}
