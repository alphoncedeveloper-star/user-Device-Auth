package com.alevo.usermodule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_token", columnList = "token", unique = true),
        @Index(name = "idx_rt_user", columnList = "user_id"),
        @Index(name = "idx_rt_device", columnList = "device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoke_reason", length = 100)
    private String revokeReason;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke(String reason) {
        this.revoked = true;
        this.revokeReason = reason;
    }

    public void touch() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
