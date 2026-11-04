package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.entity.EmailOtp;
import com.prepmind.prepapi.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);

    Optional<EmailOtp> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, OtpPurpose purpose, Instant after);

    @Modifying
    @Query("UPDATE EmailOtp o SET o.consumedAt = :now "
            + "WHERE o.user.id = :userId AND o.purpose = :purpose AND o.consumedAt IS NULL")
    int consumeAllActive(@Param("userId") Long userId, @Param("purpose") OtpPurpose purpose, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
