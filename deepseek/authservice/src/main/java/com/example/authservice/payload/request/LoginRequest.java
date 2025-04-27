package com.example.authservice.payload.request;

import jakarta.validation.constraints.NotBlank; // Fixed import
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}