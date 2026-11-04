package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** Loads every question together with its companies in one query (no N+1). */
    @EntityGraph(attributePaths = "companies")
    @Query("SELECT DISTINCT q FROM Question q ORDER BY q.id")
    List<Question> findAllWithCompanies();

    boolean existsByProblemUrl(String problemUrl);

    boolean existsByProblemUrlAndIdNot(String problemUrl, Long id);

    boolean existsBySectionAndTitleIgnoreCaseAndIdNot(String section, String title, Long id);

    @Query("SELECT q.section, COUNT(q) FROM Question q GROUP BY q.section")
    List<Object[]> countBySection();

    boolean existsBySectionAndTitleIgnoreCase(String section, String title);

    /** Natural keys of every question, used by the seeder to skip ones that already exist. */
    @Query("SELECT q.section, q.title, q.problemUrl FROM Question q")
    List<Object[]> findAllSeedKeys();

    /** v3 upgrade: questions created before sections existed are DSA problems. */
    @Modifying
    @Query("UPDATE Question q SET q.section = 'DSA' WHERE q.section IS NULL")
    int assignDsaToUnsectioned();
}
