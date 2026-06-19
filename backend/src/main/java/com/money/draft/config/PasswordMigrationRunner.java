
package com.money.draft.config;

import com.money.draft.domain.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(AppUserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var users = userRepo.findAll();
        int migrated = 0;

        for (var user : users) {
            String pw = user.getPassword();
            if (pw != null && !pw.startsWith("$2a$") && !pw.startsWith("$2b$") && !pw.startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(pw));
                userRepo.save(user);
                migrated++;
                log.info("Migrated password for user: {}", user.getUsername());
            }
        }

        if (migrated > 0) {
            log.info("Password migration complete. {} user(s) updated to BCrypt.", migrated);
        } else {
            log.info("Password migration: no plaintext passwords found.");
        }
    }
}
