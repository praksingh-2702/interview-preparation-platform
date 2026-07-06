package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<Progress, Long> {
    
    // This allows Spring to automatically generate: SELECT * FROM user_progress WHERE user_id = ? AND question_id = ?
    Optional<Progress> findByUserIdAndQuestionId(Long userId, Long questionId);
    
    // Useful for fetching all tracked progress rows when loading the dashboard state later
    List<Progress> findByUserId(Long userId);
}