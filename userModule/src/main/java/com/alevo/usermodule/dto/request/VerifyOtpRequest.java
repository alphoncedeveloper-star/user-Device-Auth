package com.alevo.usermodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone must be E.164 format")
    private String phoneNumber;

    @NotBlank
    @Size(min = 4, max = 8)
    private String otpCode;

    @NotBlank
    private String deviceName;

    @NotBlank
    private String deviceType; // ANDROID | IOS | WEB | DESKTOP_WINDOWS | DESKTOP_MAC | DESKTOP_LINUX

    private String osName;
    private String osVersion;
    private String browserOrApp;
    private String appVersion;

    /** SHA-256 of device hardware IDs / browser fingerprint */
    @NotBlank
    @Size(min = 32, max = 64)
    private String fingerprint;

    private String pushToken;
}