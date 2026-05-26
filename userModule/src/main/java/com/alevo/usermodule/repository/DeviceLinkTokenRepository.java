package com.alevo.usermodule.repository;


import com.alevo.usermodule.entity.DeviceLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeviceLinkTokenRepository extends JpaRepository<DeviceLinkToken, String> {

    Optional<DeviceLinkToken> findByLinkTokenAndUsedFalse(String linkToken);

    @Modifying
    @Query("DELETE FROM DeviceLinkToken d WHERE d.expiresAt < :before")
    void deleteExpiredTokens(LocalDateTime before);
}