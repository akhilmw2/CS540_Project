package com.example.authservice.payload.request;

import jakarta.validation.constraints.NotBlank; // Fixed import
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}