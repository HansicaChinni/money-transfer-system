package com.money.draft;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class DraftApplication {

    private static final Logger log = LoggerFactory.getLogger(DraftApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DraftApplication.class, args);
    }

    @Bean
    public CommandLineRunner migratePasswords(AppUserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            for (AppUser user : userRepo.findAll()) {
                if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                    log.info("Re-encoding plaintext password for user: {}", user.getUsername());
                    user.setPassword(encoder.encode(user.getPassword()));
                    userRepo.save(user);
                }
            }
        };
    }
}
