package com.alevo.usermodule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse {
    private String id;
    private String deviceName;
    private String deviceType;
    private String osName;
    private String osVersion;
    private String browserOrApp;
    private String appVersion;
    private boolean primary;
    private boolean active;
    private String ipAddress;
    private LocalDateTime lastActiveAt;
    private LocalDateTime linkedAt;
    private boolean currentDevice;
}