package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** Loads every question together with its companies in one query (no N+1). */
    @EntityGraph(attributePaths = "companies")
    @Query("SELECT DISTINCT q FROM Question q ORDER BY q.id")
    List<Question> findAllWithCompanies();

    boolean existsByProblemUrl(String problemUrl);

    @Query("SELECT q.problemUrl FROM Question q WHERE q.problemUrl IS NOT NULL")
    List<String> findAllProblemUrls();
}
