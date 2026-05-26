package com.alevo.usermodule.service;

import com.alevo.usermodule.dto.request.AddDeviceRequest;
import com.alevo.usermodule.dto.request.UpdateDeviceRequest;
import com.alevo.usermodule.dto.response.AuthResponse;
import com.alevo.usermodule.dto.response.DeviceLinkResponse;
import com.alevo.usermodule.dto.response.DeviceResponse;
import com.alevo.usermodule.entity.Device;
import com.alevo.usermodule.entity.DeviceLinkToken;
import com.alevo.usermodule.entity.RefreshToken;
import com.alevo.usermodule.entity.UserAccount;
import com.alevo.usermodule.enums.DeviceType;
import com.alevo.usermodule.exception.AuthException;
import com.alevo.usermodule.exception.DeviceLimitException;
import com.alevo.usermodule.exception.ResourceNotFoundException;
import com.alevo.usermodule.repository.DeviceLinkTokenRepository;
import com.alevo.usermodule.repository.DeviceRepository;
import com.alevo.usermodule.repository.RefreshTokenRepository;
import com.alevo.usermodule.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceLinkTokenRepository deviceLinkTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    @Value("${device.max-linked-devices:4}")
    private int maxLinkedDevices;

    @Value("${jwt.device-link-token-expiry-ms:300000}")
    private long deviceLinkTokenExpiryMs;

    /**
     * List all active linked devices for a user.
     */
    public List<DeviceResponse> listDevices(UserAccount user, String currentDeviceId) {
        return deviceRepository.findByUserAndActiveTrue(user).stream().map(d -> authService.mapDevice(d, currentDeviceId)).toList();
    }

    /**
     * Generate a pairing token for linking a new device (QR code / link code flow).
     * The authenticated primary device calls this; the new device scans/enters the code.
     */
    @Transactional
    public DeviceLinkResponse generateLinkToken(UserAccount user) {
        long activeCount = deviceRepository.countByUserAndActiveTrue(user);
        if (activeCount >= maxLinkedDevices) {
            throw new DeviceLimitException("You have reached the limit of " + maxLinkedDevices + " linked devices. Please remove a device first.");
        }

        String linkToken = tokenProvider.generateDeviceLinkToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(deviceLinkTokenExpiryMs * 1_000_000);

        DeviceLinkToken token = DeviceLinkToken.builder().linkToken(linkToken).user(user).expiresAt(expiresAt).build();
        deviceLinkTokenRepository.save(token);

        // QR content is a deep-link the new device can scan
        String qrContent = "userauth://link?token=" + linkToken + "&expires=" + expiresAt;
        int expiresInSeconds = (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);

        log.info("Device link token generated for user {}: {}", user.getPhoneNumber(), linkToken);

        return DeviceLinkResponse.builder().linkToken(linkToken).qrContent(qrContent).expiresAt(expiresAt).expiresInSeconds(expiresInSeconds).build();
    }

    /**
     * New device submits a link token (scanned from QR or entered manually).
     * Returns full auth credentials for the new device.
     */
    @Transactional
    public AuthResponse completeLinkDevice(AddDeviceRequest req) {
        DeviceLinkToken linkToken = deviceLinkTokenRepository.findByLinkTokenAndUsedFalse(req.getLinkToken()).orElseThrow(() -> new AuthException("Invalid or expired device link token"));

        if (!linkToken.isValid()) {
            throw new AuthException("Device link token has expired. Please generate a new one.");
        }

        UserAccount user = linkToken.getUser();
        long activeCount = deviceRepository.countByUserAndActiveTrue(user);

        if (activeCount >= maxLinkedDevices) {
            throw new DeviceLimitException("Device limit reached. Please remove a device from your primary device.");
        }

        // Check for existing fingerprint
        Optional<Device> existing = deviceRepository.findByFingerprintAndUser(req.getFingerprint(), user);
        Device device;
        if (existing.isPresent()) {
            device = existing.get();
            device.setActive(true);
            device.setUnlinkedAt(null);
            device.setUnlinkReason(null);
            device.setPushToken(req.getPushToken());
            device.touch();
            device = deviceRepository.save(device);
        } else {
            device = Device.builder().user(user).deviceName(req.getDeviceName()).deviceType(DeviceType.valueOf(req.getDeviceType())).osName(req.getOsName()).osVersion(req.getOsVersion()).browserOrApp(req.getBrowserOrApp()).appVersion(req.getAppVersion()).fingerprint(req.getFingerprint()).pushToken(req.getPushToken()).primary(false).lastActiveAt(LocalDateTime.now()).build();
            device = deviceRepository.save(device);
        }

        // Mark link token as used
        linkToken.markUsed();
        deviceLinkTokenRepository.save(linkToken);

        // Issue tokens
        String accessToken = tokenProvider.generateAccessToken(user, device);
        String rawRefreshToken = tokenProvider.generateRefreshToken();

        // Revoke any old token for this device
        refreshTokenRepository.findByDeviceAndRevokedFalse(device).ifPresent(t -> {
            t.revoke("DEVICE_RE_LINKED");
            refreshTokenRepository.save(t);
        });

        RefreshToken refreshToken = RefreshToken.builder().token(rawRefreshToken).user(user).device(device).expiresAt(LocalDateTime.now().plusNanos(tokenProvider.getRefreshTokenExpiryMs() * 1_000_000)).build();
        refreshTokenRepository.save(refreshToken);

        log.info("Device {} linked for user {}", device.getDeviceName(), user.getPhoneNumber());

        return AuthResponse.builder().accessToken(accessToken).refreshToken(rawRefreshToken).accessTokenExpiresIn(900).user(authService.mapUser(user)).device(authService.mapDevice(device, device.getId())).build();
    }

    /**
     * Unlink (revoke) a specific device.
     * A device can unlink itself, or primary can unlink others.
     */
    @Transactional
    public void unlinkDevice(UserAccount user, String targetDeviceId, Device requestingDevice) {
        Device target = deviceRepository.findByIdAndUser(targetDeviceId, user).orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (!target.isActive()) {
            throw new AuthException("Device is already unlinked");
        }

        boolean isSelf = target.getId().equals(requestingDevice.getId());
        boolean isPrimary = requestingDevice.isPrimary();

        if (!isSelf && !isPrimary) {
            throw new AuthException("Only the primary device can unlink other devices");
        }

        refreshTokenRepository.revokeDeviceTokens(target, "DEVICE_UNLINKED");
        target.deactivate("DEVICE_UNLINKED");
        deviceRepository.save(target);

        log.info("Device {} unlinked from user {} by device {}", target.getDeviceName(), user.getPhoneNumber(), requestingDevice.getDeviceName());
    }

    /**
     * Update device metadata (name, push token).
     */
    @Transactional
    public DeviceResponse updateDevice(UserAccount user, String deviceId, UpdateDeviceRequest req) {
        Device device = deviceRepository.findByIdAndUser(deviceId, user).orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        if (req.getDeviceName() != null) device.setDeviceName(req.getDeviceName());
        if (req.getPushToken() != null) device.setPushToken(req.getPushToken());

        return authService.mapDevice(deviceRepository.save(device), deviceId);
    }
}
