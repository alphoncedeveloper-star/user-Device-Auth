package com.alevo.usermodule.service;

import com.alevo.usermodule.entity.OtpToken;
import com.alevo.usermodule.enums.OtpPurpose;
import com.alevo.usermodule.exception.OtpException;
import com.alevo.usermodule.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    /**
     * Generate and send an OTP to a phone number.
     * Enforces rate limiting: max 1 OTP per cooldown window.
     */
    @Transactional
    public void sendOtp(String phoneNumber, OtpPurpose purpose) {
        // Rate limiting: check if a recent OTP was sent
        long recentCount = otpTokenRepository.countRecentOtpRequests(
                phoneNumber, LocalDateTime.now().minusSeconds(resendCooldownSeconds));

        if (recentCount > 0) {
            throw new OtpException("Please wait " + resendCooldownSeconds +
                    " seconds before requesting another OTP");
        }

        // Invalidate any existing active OTPs
        otpTokenRepository.invalidateAllActiveOtps(phoneNumber);

        // Generate new OTP
        String otpCode = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpTokenRepository.save(otpToken);

        // Send via SMS
        smsService.sendOtp(phoneNumber, otpCode);

        log.info("OTP sent to {} for purpose {}", maskPhone(phoneNumber), purpose);
    }

    /**
     * Verify an OTP. Returns true on success. Throws on failure.
     */
    @Transactional
    public void verifyOtp(String phoneNumber, String otpCode, OtpPurpose purpose) {
        OtpToken otpToken = otpTokenRepository
                .findTopByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(phoneNumber, purpose)
                .orElseThrow(() -> new OtpException("No active OTP found. Please request a new one."));

        if (otpToken.isExpired()) {
            throw new OtpException("OTP has expired. Please request a new one.");
        }

        if (otpToken.getAttempts() >= 3) {
            throw new OtpException("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!otpToken.getOtpCode().equals(otpCode)) {
            otpToken.incrementAttempts();
            otpTokenRepository.save(otpToken);
            int remaining = 3 - otpToken.getAttempts();
            throw new OtpException("Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        otpToken.markUsed();
        otpTokenRepository.save(otpToken);
        log.info("OTP verified for {}", maskPhone(phoneNumber));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
