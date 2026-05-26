package com.alevo.usermodule.controller;

import com.alevo.usermodule.dto.request.RefreshTokenRequest;
import com.alevo.usermodule.dto.request.SendOtpRequest;
import com.alevo.usermodule.dto.request.VerifyOtpRequest;
import com.alevo.usermodule.dto.response.ApiResponse;
import com.alevo.usermodule.dto.response.AuthResponse;
import com.alevo.usermodule.security.AuthenticatedPrincipal;
import com.alevo.usermodule.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Public ─────────────────────────────────────────────────

    /**
     * Step 1: Request an OTP.
     * Works for both new registrations and existing logins.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(
                "OTP sent to " + maskPhone(req.getPhoneNumber()) + ". Valid for 10 minutes."));
    }

    /**
     * Step 2: Verify OTP and register device.
     * Returns access token, refresh token, user profile, and device info.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req) {
        AuthResponse auth = authService.verifyOtpAndAuthenticate(req);
        String msg = auth.isNewUser() ? "Welcome! Account created." : "Login successful.";
        return ResponseEntity.ok(ApiResponse.ok(msg, auth));
    }

    /**
     * Refresh an expired access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        AuthResponse auth = authService.refreshAccessToken(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", auth));
    }

    // ── Protected ──────────────────────────────────────────────

    /**
     * Logout from current device only.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        authService.logout(principal.getUser(), principal.getDevice());
        return ResponseEntity.ok(ApiResponse.ok("Logged out from this device"));
    }

    /**
     * Logout from ALL linked devices.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        authService.logoutAllDevices(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices"));
    }

    /**
     * Verify token and return current auth status.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Authenticated", AuthResponse.builder()
                .user(authService.mapUser(principal.getUser()))
                .device(authService.mapDevice(principal.getDevice(), principal.getDeviceId()))
                .accessTokenExpiresIn(900)
                .build()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
