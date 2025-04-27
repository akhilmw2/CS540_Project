package com.example.authservice.controller;

import com.example.authservice.payload.request.ChangePasswordRequest;
import com.example.authservice.payload.response.MessageResponse;
import com.example.authservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth") // Changed from "/api/auth"
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }
}