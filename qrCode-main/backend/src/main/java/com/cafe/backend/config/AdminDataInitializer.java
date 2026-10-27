package com.cafe.backend.config;

import com.cafe.backend.entity.Admin;
import com.cafe.backend.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
@Configuration
@Profile("dev")
public class AdminDataInitializer {

    @Bean
    CommandLineRunner createInitialAdmin(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            if (adminRepository.existsByEmail("admin@rushhour.com")) {
                return;
            }

            Admin admin = new Admin();

            admin.setName("RushHour Admin");
            admin.setEmail("admin@rushhour.com");
            admin.setPasswordHash(
                    passwordEncoder.encode("Admin@12345")
            );
            admin.setRole("ADMIN");
            admin.setActive(true);

            adminRepository.save(admin);

            System.out.println("Initial admin created successfully.");
        };
    }
}