package com.alevo.usermodule.entity;

import com.alevo.usermodule.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens", indexes = {@Index(name = "idx_otp_phone", columnList = "phone_number"), @Index(name = "idx_otp_expires", columnList = "expires_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.LOGIN;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired() && attempts < 3;
    }

    public void markUsed() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }

    public void incrementAttempts() {
        this.attempts++;
    }


}
