package com.alevo.usermodule.dto.request;

import lombok.Data;

@Data
public class UpdateDeviceRequest {
    private String deviceName;
    private String pushToken;
}
