package com.pangasmart.pangasmart.config;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("kkisanga009@gmail.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("kkisanga009@gmail.com");
            admin.setPassword("admin123"); // Password yako ya kuingilia
            admin.setRole("ADMIN");

            // Weka namba ya simu kama User model yako ina getPhone() au setPhone()
            try {
                admin.setPhone("0700000000");
            } catch (Exception e) {
                System.out.println("No phone field in User model");
            }

            userRepository.save(admin);
            System.out.println("✅ Admin account created successfully!");
        }
    }
}