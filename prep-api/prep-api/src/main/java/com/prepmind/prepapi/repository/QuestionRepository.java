package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByDifficulty(String difficulty);

    // Custom query to search inside the Set of companies
    @Query("SELECT q FROM Question q JOIN q.companies c WHERE LOWER(c) = LOWER(:company)")
    List<Question> findByCompanyName(@Param("company") String company);
}