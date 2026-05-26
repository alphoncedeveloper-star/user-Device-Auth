package com.alevo.usermodule.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class DeviceLinkResponse {
    private String linkToken;
    private String qrContent;
    private LocalDateTime expiresAt;
    private int expiresInSeconds;
}