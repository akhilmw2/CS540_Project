package com.example.authservice;

import com.example.authservice.model.ERole;
import com.example.authservice.model.Role;
import com.example.authservice.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthserviceApplication {
	@Autowired
	RoleRepository roleRepository;

	public static void main(String[] args) {
		SpringApplication.run(AuthserviceApplication.class, args);
	}

	@PostConstruct
	public void initRoles() {
		if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
			roleRepository.save(new Role(ERole.ROLE_USER)); // Now uses correct constructor
		}
		if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
			roleRepository.save(new Role(ERole.ROLE_ADMIN));
		}
	}
}