package com.pangasmart.pangasmart.config;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitAdmin{

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByEmail("kkisanga009@gmail.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("kkisanga009@gmail.com");
                admin.setPassword("admin@45"); // Password yako ya kuingilia
                admin.setRole("ADMIN");
                admin.setPhone("0747466962");
                userRepository.save(admin);
                System.out.println("✅ Admin account created successfully!");
            }
        };
    };
}