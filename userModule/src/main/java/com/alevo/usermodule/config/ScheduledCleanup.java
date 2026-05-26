package com.alevo.usermodule.config;

import com.alevo.usermodule.repository.DeviceLinkTokenRepository;
import com.alevo.usermodule.repository.OtpTokenRepository;
import com.alevo.usermodule.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled jobs to keep the database clean.
 * Runs at 3 AM daily.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanup {

    private final OtpTokenRepository otpTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceLinkTokenRepository deviceLinkTokenRepository;

    @Scheduled(cron = "0 * * * * *") // 03:00 daily
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        int otpsDeleted = (int) otpTokenRepository.count();
        otpTokenRepository.deleteExpiredOtps(now);
        log.info("Cleaned expired OTPs");

        refreshTokenRepository.deleteExpiredTokens(now);
        log.info("Cleaned expired refresh tokens");

        deviceLinkTokenRepository.deleteExpiredTokens(now);
        log.info("Cleaned expired device link tokens");
    }
}