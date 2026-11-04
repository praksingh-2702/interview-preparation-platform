package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.ProgressEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProgressEventRepository extends JpaRepository<ProgressEvent, Long> {

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM ProgressEvent e WHERE e.createdAt >= :since")
    long countActiveUsersSince(@Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM ProgressEvent e WHERE e.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ProgressEvent e WHERE e.question.id = :questionId")
    int deleteByQuestionId(@Param("questionId") Long questionId);
}
