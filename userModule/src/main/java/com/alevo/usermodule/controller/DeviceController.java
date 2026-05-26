package com.alevo.usermodule.controller;


import com.alevo.usermodule.dto.request.AddDeviceRequest;
import com.alevo.usermodule.dto.request.UpdateDeviceRequest;
import com.alevo.usermodule.dto.response.ApiResponse;
import com.alevo.usermodule.dto.response.AuthResponse;
import com.alevo.usermodule.dto.response.DeviceLinkResponse;
import com.alevo.usermodule.dto.response.DeviceResponse;
import com.alevo.usermodule.security.AuthenticatedPrincipal;
import com.alevo.usermodule.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Device management — WhatsApp linked-devices style.
 * <p>
 * GET    /api/devices                → List all linked devices
 * POST   /api/devices/link/generate  → Generate QR/link token (from primary device)
 * POST   /api/devices/link/verify    → Submit link token (from new device) → returns tokens
 * DELETE /api/devices/{id}           → Unlink a specific device
 * PATCH  /api/devices/{id}           → Update device name / push token
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * List all active linked devices for the authenticated user.
     * The requesting device is flagged with currentDevice=true.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> listDevices(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<DeviceResponse> devices = deviceService.listDevices(principal.getUser(), principal.getDeviceId());
        return ResponseEntity.ok(ApiResponse.ok(devices.size() + " device(s) linked", devices));
    }

    /**
     * Generate a device-link token (QR code / pairing code).
     * Called by an already-authenticated device. The new device scans or enters the code.
     * Token expires in 5 minutes.
     */
    @PostMapping("/link/generate")
    public ResponseEntity<ApiResponse<DeviceLinkResponse>> generateLinkToken(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        DeviceLinkResponse response = deviceService.generateLinkToken(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("Scan the QR or enter the link code on your new device. Expires in 5 minutes.", response));
    }

    /**
     * Complete device linking — called by the NEW device after scanning/entering the token.
     * No JWT required; the link token authorizes this request.
     * Returns full auth credentials for the new device.
     */
    @PostMapping("/link/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> completeLinkDevice(@Valid @RequestBody AddDeviceRequest req) {
        AuthResponse auth = deviceService.completeLinkDevice(req);
        return ResponseEntity.ok(ApiResponse.ok("Device linked successfully. You are now logged in.", auth));
    }

    /**
     * Unlink (logout) a specific device by ID.
     * A device may unlink itself; only primary devices can unlink others.
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> unlinkDevice(@PathVariable String deviceId, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        deviceService.unlinkDevice(principal.getUser(), deviceId, principal.getDevice());
        return ResponseEntity.ok(ApiResponse.ok("Device unlinked successfully"));
    }

    /**
     * Update device name or push notification token.
     */
    @PatchMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(@PathVariable String deviceId, @Valid @RequestBody UpdateDeviceRequest req, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        DeviceResponse updated = deviceService.updateDevice(principal.getUser(), deviceId, req);
        return ResponseEntity.ok(ApiResponse.ok("Device updated", updated));
    }
}
