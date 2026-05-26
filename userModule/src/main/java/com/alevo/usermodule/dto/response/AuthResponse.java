package com.alevo.usermodule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private UserResponse user;
    private DeviceResponse device;
    private boolean newUser;
}