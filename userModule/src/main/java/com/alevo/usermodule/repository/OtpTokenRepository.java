package com.alevo.usermodule.repository;

import com.alevo.usermodule.entity.OtpToken;
import com.alevo.usermodule.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String phoneNumber, OtpPurpose purpose);

    @Query("SELECT COUNT(o) FROM OtpToken o WHERE o.phoneNumber = :phone AND o.createdAt > :since")
    long countRecentOtpRequests(String phone, LocalDateTime since);

    @Modifying
    @Query("UPDATE OtpToken o SET o.used = true WHERE o.phoneNumber = :phone AND o.used = false")
    void invalidateAllActiveOtps(String phone);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :before")
    void deleteExpiredOtps(LocalDateTime before);
}
