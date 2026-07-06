package com.prepmind.prepapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProgressRepository extends JpaRepository<com.prepmind.prepapi.entity.Progress, Long> {
    // Spring Data JPA handles all standard CRUD operations, including deleteAll()
}