package com.example.authservice.service;

import com.example.authservice.model.ERole;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.payload.request.RegisterRequest;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setEnabled(false);
        user.setConfirmationToken(UUID.randomUUID().toString());

        Optional<Role> userRole = roleRepository.findByName(ERole.ROLE_USER);
        user.getRoles().add(userRole.orElseThrow(() -> new RuntimeException("Role not found")));

        System.out.println("Confirmation URL: http://localhost:8082/api/auth/confirm?token=" + user.getConfirmationToken());
        return userRepository.save(user);
    }

    public void confirmEmail(String token) {
        User user = userRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        user.setEnabled(true);
        user.setConfirmationToken(null);
        userRepository.save(user);
    }
}