package com.alevo.usermodule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private String id;
    private String phoneNumber;
    private String displayName;
    private String about;
    private String profilePictureUrl;
    private boolean verified;
    private String status;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}