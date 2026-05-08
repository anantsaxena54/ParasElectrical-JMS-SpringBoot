package com.paraselectricals.jms_backend.config;

import com.paraselectricals.jms_backend.entity.User;
import com.paraselectricals.jms_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User defaultUser = new User();
                defaultUser.setUsername("admin");
                defaultUser.setPassword("admin123");
                userRepository.save(defaultUser);
                System.out.println("Default user 'admin' created.");
            }
        };
    }
}
