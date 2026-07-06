package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    // Finds a specific progress record to see if we need to create or update
    Optional<Progress> findByUserAndQuestion(User user, Question question);
    
    // We will use this later for the user dashboard to get all their solved problems
    List<Progress> findByUser_Id(Long userId); 
}