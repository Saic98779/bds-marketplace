package com.sidbi.bds.auth.repository;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findFirstByTargetHashAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String targetHash, Enums.OtpPurpose purpose);

    Optional<OtpToken> findByChallengeHashAndConsumedFalse(String challengeHash);

    @Modifying
    @Query("update OtpToken o set o.consumed = true where o.targetHash = :target and o.purpose = :purpose and o.consumed = false")
    void invalidate(@Param("target") String targetHash, @Param("purpose") Enums.OtpPurpose purpose);

    @Modifying
    @Query("delete from OtpToken o where o.expiresAt < :before")
    void purgeExpired(@Param("before") LocalDateTime before);
}
