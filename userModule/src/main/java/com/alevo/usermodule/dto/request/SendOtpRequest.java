package com.alevo.usermodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone must be E.164 format e.g. +255712345678")
    private String phoneNumber;

    private String purpose = "LOGIN"; // LOGIN | REGISTRATION
}