package com.alevo.usermodule.service;

import com.alevo.usermodule.dto.request.VerifyOtpRequest;
import com.alevo.usermodule.dto.response.AuthResponse;
import com.alevo.usermodule.dto.response.DeviceResponse;
import com.alevo.usermodule.dto.response.UserResponse;
import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.RefreshToken;
import com.alevo.usermodule.entity.UserAccount;
import com.alevo.usermodule.enums.DeviceType;
import com.alevo.usermodule.enums.OtpPurpose;
import com.alevo.usermodule.exception.AuthException;
import com.alevo.usermodule.exception.DeviceLimitException;
import com.alevo.usermodule.repository.DeviceRepository;
import com.alevo.usermodule.repository.RefreshTokenRepository;
import com.alevo.usermodule.repository.UserRepository;
import com.alevo.usermodule.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtTokenProvider tokenProvider;

    @Value("${device.max-linked-devices:4}")
    private int maxLinkedDevices;

    /**
     * Send OTP - works for both new and existing users.
     */
    public void sendOtp(String phoneNumber) {
        boolean isNew = !userRepository.existsByPhoneNumber(phoneNumber);
        OtpPurpose purpose = isNew ? OtpPurpose.REGISTRATION : OtpPurpose.LOGIN;
        otpService.sendOtp(phoneNumber, purpose);
    }

    /**
     * Verify OTP and return tokens. Creates user if new, registers device.
     */
    @Transactional
    public AuthResponse verifyOtpAndAuthenticate(VerifyOtpRequest req) {
        boolean isNew = !userRepository.existsByPhoneNumber(req.getPhoneNumber());
        OtpPurpose purpose = isNew ? OtpPurpose.REGISTRATION : OtpPurpose.LOGIN;

        // Verify OTP (throws on failure)
        otpService.verifyOtp(req.getPhoneNumber(), req.getOtpCode(), purpose);

        // Get or create user
        UserAccount user = userRepository.findByPhoneNumber(req.getPhoneNumber()).orElseGet(() -> createNewUser(req.getPhoneNumber()));

        // Register or re-activate device
        Device device = resolveDevice(user, req, isNew);

        // Issue tokens
        String accessToken = tokenProvider.generateAccessToken(user, device);
        String rawRefreshToken = tokenProvider.generateRefreshToken();
        issueRefreshToken(user, device, rawRefreshToken);

        log.info("User {} authenticated via device {} (new={})", user.getPhoneNumber(), device.getDeviceName(), isNew);

        return AuthResponse.builder().accessToken(accessToken).refreshToken(rawRefreshToken).accessTokenExpiresIn(900) // 15 min in seconds
                .user(mapUser(user)).device(mapDevice(device, device.getId())).newUser(isNew).build();
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(rawRefreshToken).orElseThrow(() -> new AuthException("Invalid or expired refresh token"));

        if (!refreshToken.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        UserAccount user = refreshToken.getUser();
        Device device = refreshToken.getDevice();

        if (!user.isActive()) throw new AuthException("Account is deactivated");
        if (!device.isActive()) throw new AuthException("Device has been unlinked");

        refreshToken.touch();
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user, device);

        return AuthResponse.builder().accessToken(newAccessToken).refreshToken(rawRefreshToken) // same refresh token
                .accessTokenExpiresIn(900).user(mapUser(user)).device(mapDevice(device, device.getId())).build();
    }

    /**
     * Logout from current device — revoke its tokens.
     */
    @Transactional
    public void logout(UserAccount user, Device device) {
        refreshTokenRepository.revokeDeviceTokens(device, "USER_LOGOUT");
        device.deactivate("USER_LOGOUT");
        deviceRepository.save(device);
        log.info("User {} logged out from device {}", user.getPhoneNumber(), device.getDeviceName());
    }

    /**
     * Logout from ALL devices.
     */
    @Transactional
    public void logoutAllDevices(UserAccount user) {
        refreshTokenRepository.revokeAllUserTokens(user, "LOGOUT_ALL_DEVICES");
        deviceRepository.deactivateAllNonPrimaryDevices(user, "LOGOUT_ALL_DEVICES");

        // Also deactivate primary device
        Optional<Device> primary = deviceRepository.findByUserAndPrimaryTrue(user);
        primary.ifPresent(d -> {
            d.deactivate("LOGOUT_ALL_DEVICES");
            deviceRepository.save(d);
        });

        log.info("All devices logged out for user {}", user.getPhoneNumber());
    }

    // ─── Helpers ─────────────────────────────────────────────

    private UserAccount createNewUser(String phoneNumber) {
        UserAccount user = UserAccount.builder().phoneNumber(phoneNumber).verified(true).build();
        return userRepository.save(user);
    }

    private Device resolveDevice(UserAccount user, VerifyOtpRequest req, boolean isFirstDevice) {
        // Check if same device fingerprint already exists
        Optional<Device> existing = deviceRepository.findByFingerprintAndUser(req.getFingerprint(), user);
        if (existing.isPresent()) {
            Device device = existing.get();
            device.setActive(true);
            device.setUnlinkedAt(null);
            device.setUnlinkReason(null);
            device.setPushToken(req.getPushToken());
            device.touch();
            return deviceRepository.save(device);
        }

        // Check device limit
        long activeCount = deviceRepository.countByUserAndActiveTrue(user);
        if (activeCount >= maxLinkedDevices) {
            throw new DeviceLimitException("Maximum of " + maxLinkedDevices + " devices allowed. Please unlink a device first.");
        }

        String ip = extractIpAddress();
        Device device = Device.builder().user(user).deviceName(req.getDeviceName()).deviceType(DeviceType.valueOf(req.getDeviceType())).osName(req.getOsName()).osVersion(req.getOsVersion()).browserOrApp(req.getBrowserOrApp()).appVersion(req.getAppVersion()).fingerprint(req.getFingerprint()).pushToken(req.getPushToken()).ipAddress(ip).primary(isFirstDevice || activeCount == 0).lastActiveAt(LocalDateTime.now()).build();

        return deviceRepository.save(device);
    }

    private void issueRefreshToken(UserAccount user, Device device, String rawToken) {
        // Revoke any existing token for this device
        refreshTokenRepository.findByDeviceAndRevokedFalse(device).ifPresent(t -> {
            t.revoke("NEW_LOGIN");
            refreshTokenRepository.save(t);
        });

        RefreshToken refreshToken = RefreshToken.builder().token(rawToken).user(user).device(device).expiresAt(LocalDateTime.now().plusNanos(tokenProvider.getRefreshTokenExpiryMs() * 1_000_000)).build();
        refreshTokenRepository.save(refreshToken);
    }

    private String extractIpAddress() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String xff = attrs.getRequest().getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    public UserResponse mapUser(UserAccount user) {
        return UserResponse.builder().id(user.getId()).phoneNumber(user.getPhoneNumber()).displayName(user.getDisplayName()).about(user.getAbout()).profilePictureUrl(user.getProfilePictureUrl()).verified(user.isVerified()).status(user.getStatus().name()).lastSeen(user.getLastSeen()).createdAt(user.getCreatedAt()).build();
    }

    public DeviceResponse mapDevice(Device device, String currentDeviceId) {
        return DeviceResponse.builder().id(device.getId()).deviceName(device.getDeviceName()).deviceType(device.getDeviceType().name()).osName(device.getOsName()).osVersion(device.getOsVersion()).browserOrApp(device.getBrowserOrApp()).appVersion(device.getAppVersion()).primary(device.isPrimary()).active(device.isActive()).ipAddress(device.getIpAddress()).lastActiveAt(device.getLastActiveAt()).linkedAt(device.getLinkedAt()).currentDevice(device.getId().equals(currentDeviceId)).build();
    }
}
