package com.example.authservice.controller;

import org.springframework.http.ResponseEntity;
import com.example.authservice.payload.request.LoginRequest;
import com.example.authservice.payload.request.RegisterRequest;
import com.example.authservice.payload.response.JwtResponse;
import com.example.authservice.payload.response.MessageResponse;
import com.example.authservice.security.jwt.JwtUtils;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth") // Changed from "/api/auth
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    AuthService authService;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse("User registered successfully. Check console for confirmation URL."));
    }

    @GetMapping("/confirm")
    public ResponseEntity<?> confirmEmail(@RequestParam String token) {
        authService.confirmEmail(token);
        return ResponseEntity.ok(new MessageResponse("Email confirmed successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return ResponseEntity.ok(new JwtResponse(jwt));
    }



    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully. Please remove token on client side."));
    }
}