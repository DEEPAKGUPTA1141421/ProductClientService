package com.ProductClientService.ProductClientService.Service.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ProductClientService.ProductClientService.Model.AdminUser;
import com.ProductClientService.ProductClientService.Repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds a single default admin account for local/demo use, purely so the
 * Admin Portal has something to log in with out of the box.
 *
 * No Spring profiles are used elsewhere in this codebase to gate beans, so
 * this simply follows the existing "insert if not exists" convention: it is
 * a no-op once an admin_users row already exists (including one created
 * manually in a real environment), so it's safe to leave enabled everywhere.
 *
 * Default credentials (change immediately in any non-local environment):
 *   email:    admin@marketplace.example
 *   password: Admin@12345
 */
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserSeeder.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@marketplace.example";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@12345";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminUserRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            return;
        }

        AdminUser admin = AdminUser.builder()
                .name("Default Admin")
                .email(DEFAULT_ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .role("ADMIN")
                .status("ACTIVE")
                .build();

        adminUserRepository.save(admin);
        logger.info("Seeded default admin user ({}) — change the password before going to production.",
                DEFAULT_ADMIN_EMAIL);
    }
}
