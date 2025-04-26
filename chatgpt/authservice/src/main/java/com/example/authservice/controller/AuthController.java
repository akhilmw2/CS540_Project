
package com.example.authservice.controller;

import com.example.authservice.model.User;
import com.example.authservice.payload.*;
import com.example.authservice.security.UserDetailsImpl;
import com.example.authservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private com.example.authservice.security.JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;  // ✅ Add this

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String result = userService.registerUser(request);
        if (result.contains("exists")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirmUser(@RequestParam("token") String token) {
        boolean confirmed = userService.confirmUser(token);
        if (confirmed) {
            return ResponseEntity.ok("Email confirmed successfully!");
        }
        return ResponseEntity.badRequest().body("Invalid or expired token.");
    }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        String email = auth.getName();
        String result = userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());

        if (result.equals("Password changed successfully.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("Login request: " + request);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());
            return ResponseEntity.ok("Login successful: " + jwt);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Invalid email or password");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Something went wrong during login");
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = auth.getName();
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.saveUser(user);

        return ResponseEntity.ok("Password changed successfully");
    }
}
