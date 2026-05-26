package com.alevo.usermodule.repository;

import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.RefreshToken;
import com.alevo.usermodule.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findByDeviceAndRevokedFalse(Device device);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokeReason = :reason " +
            "WHERE r.user = :user AND r.revoked = false")
    int revokeAllUserTokens(UserAccount user, String reason);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokeReason = :reason " +
            "WHERE r.device = :device AND r.revoked = false")
    int revokeDeviceTokens(Device device, String reason);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :before")
    void deleteExpiredTokens(LocalDateTime before);
}
