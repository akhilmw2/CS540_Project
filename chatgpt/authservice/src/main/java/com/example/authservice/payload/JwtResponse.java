package com.example.authservice.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class JwtResponse {
    private String token;
    private String email;
    private List<String> roles;

    public JwtResponse(String token, String email, List<String> roles) {
        this.token = token;
        this.email = email;
        this.roles = roles;
    }

    // Getters and Setters
}
