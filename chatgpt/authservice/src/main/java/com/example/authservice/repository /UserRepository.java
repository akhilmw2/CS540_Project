
package com.example.authservice.repository;

import com.example.authservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByConfirmationToken(String confirmationToken);  // ✅ New method
}
