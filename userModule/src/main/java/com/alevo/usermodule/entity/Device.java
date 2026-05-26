package com.alevo.usermodule.entity;

import com.alevo.usermodule.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_device_user", columnList = "user_id"),
        @Index(name = "idx_device_fingerprint", columnList = "fingerprint")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "device_name", nullable = false, length = 150)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "os_version", length = 30)
    private String osVersion;

    @Column(name = "browser_or_app", length = 80)
    private String browserOrApp;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    /**
     * Unique fingerprint for the device (hash of hardware/browser identifiers).
     * Used to detect duplicate device registrations.
     */
    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "push_token")
    private String pushToken;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @Column(name = "unlinked_at")
    private LocalDateTime unlinkedAt;

    @Column(name = "unlink_reason", length = 100)
    private String unlinkReason;



    /**
     * Deactivate this device with a reason.
     */
    public void deactivate(String reason) {
        this.active = false;
        this.unlinkedAt = LocalDateTime.now();
        this.unlinkReason = reason;
    }

    /**
     * Update last active timestamp.
     */
    public void touch() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
