package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.dto.ProgressView;
import com.prepmind.prepapi.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT new com.prepmind.prepapi.dto.ProgressView(p.question.id, p.solved, p.revised) "
            + "FROM Progress p WHERE p.user.id = :userId")
    List<ProgressView> findViewsByUserId(@Param("userId") Long userId);
}
