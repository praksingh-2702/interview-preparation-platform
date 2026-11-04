package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.ProgressEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProgressEventRepository extends JpaRepository<ProgressEvent, Long> {

    @Query("SELECT e.createdAt FROM ProgressEvent e WHERE e.user.id = :userId")
    List<Instant> findTimestampsByUserId(@Param("userId") Long userId);
}
