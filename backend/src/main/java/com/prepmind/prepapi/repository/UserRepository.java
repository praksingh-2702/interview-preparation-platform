package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Sign-ups that never entered their code. createdAt is null only for very old accounts. */
    @Query("SELECT u.id FROM User u WHERE (u.emailVerified IS NULL OR u.emailVerified = false) "
            + "AND (u.createdAt IS NULL OR u.createdAt < :before)")
    List<Long> findUnverifiedIdsCreatedBefore(@Param("before") Instant before);

    List<User> findTop200ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
            String username, String email);
}
