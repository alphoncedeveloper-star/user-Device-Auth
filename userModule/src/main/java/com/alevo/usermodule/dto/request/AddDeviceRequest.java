package com.alevo.usermodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddDeviceRequest {
    /** Link token from QR code scan or pairing code entry */
    @NotBlank
    private String linkToken;

    @NotBlank
    private String deviceName;

    @NotBlank
    private String deviceType;

    private String osName;
    private String osVersion;
    private String browserOrApp;
    private String appVersion;

    @NotBlank
    @Size(min = 32, max = 64)
    private String fingerprint;

    private String pushToken;
}
