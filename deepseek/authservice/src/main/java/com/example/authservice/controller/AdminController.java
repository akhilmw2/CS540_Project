package com.example.authservice.controller;

import org.springframework.http.ResponseEntity;
import com.example.authservice.payload.response.MessageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin") // Changed from "/api/auth/admin"
public class AdminController {
    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminTest() {
        return ResponseEntity.ok(new MessageResponse("Admin access successful"));
    }
}