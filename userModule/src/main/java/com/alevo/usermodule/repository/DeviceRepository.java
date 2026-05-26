package com.alevo.usermodule.repository;

import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findByUserAndActiveTrue(UserAccount user);

    long countByUserAndActiveTrue(UserAccount user);

    Optional<Device> findByIdAndUser(String id, UserAccount user);

    Optional<Device> findByFingerprintAndUser(String fingerprint, UserAccount user);

    Optional<Device> findByUserAndPrimaryTrue(UserAccount user);

    @Query("SELECT d FROM Device d WHERE d.user = :user AND d.active = true AND d.lastActiveAt < :before")
    List<Device> findInactiveDevices(UserAccount user, LocalDateTime before);

    @Modifying
    @Query("UPDATE Device d SET d.active = false, d.unlinkedAt = CURRENT_TIMESTAMP, d.unlinkReason = :reason " +
            "WHERE d.user = :user AND d.active = true AND d.primary = false")
    int deactivateAllNonPrimaryDevices(UserAccount user, String reason);
}
