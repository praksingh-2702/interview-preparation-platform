package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    Optional<User> findByGoogleSubject(String googleSubject);

    long countByEmailVerifiedTrue();

    long countByGoogleSubjectIsNotNull();

    long countByCreatedAtAfter(Instant since);

    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countByRole();

    List<User> findTop200ByOrderByCreatedAtDesc();

    List<User> findTop200ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
            String username, String email);
}
