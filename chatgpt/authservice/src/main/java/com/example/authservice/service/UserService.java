
package com.example.authservice.service;

import com.example.authservice.model.ERole;
import com.example.authservice.model.Role;
import com.example.authservice.model.User;
import com.example.authservice.payload.RegisterRequest;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    // ✅ Registration
    public String registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
            return "User with email already exists!";
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase()); // ✅ normalize email
        user.setPassword(encoder.encode(request.getPassword()));
        user.setEnabled(false); // Initially not enabled until confirmation

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role not found."));
        roles.add(userRole);
        user.setRoles(roles);

        // Generate confirmation token
        String token = UUID.randomUUID().toString();
        user.setConfirmationToken(token);

        userRepository.save(user);

        // Simulate email confirmation link (in real project, send via email)
        System.out.println("Confirmation link: http://localhost:8082/api/auth/confirm?token=" + token);

        return "User registered successfully! Please check email for confirmation link.";
    }

    // ✅ Email confirmation
    public boolean confirmUser(String token) {
        Optional<User> userOpt = userRepository.findByConfirmationToken(token);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEnabled(true);
            user.setConfirmationToken(null);
            userRepository.save(user); // ✅ Persist changes after enabling
            return true;
        }
        return false;
    }

    // ✅ Lookup by email
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.toLowerCase());
    }

    // ✅ Save updated user (e.g., password reset, profile update)
    public void saveUser(User user) {
        userRepository.save(user);
    }
    public String changePassword(String email, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) {
            return "User not found.";
        }

        User user = userOpt.get();

        // Check current password is correct
        if (!encoder.matches(currentPassword, user.getPassword())) {
            return "Current password is incorrect.";
        }

        // Set new encoded password
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        return "Password changed successfully.";
    }

}
