package com.quillforge.api.common.config;

import com.quillforge.api.user.entity.User;
import com.quillforge.api.user.entity.User.ProviderEnum;
import com.quillforge.api.user.entity.User.RoleEnum;
import com.quillforge.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "milanmitra2204@gmail.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("🚀 Seeding default admin user: {}", adminEmail);
            User admin = new User();
            admin.setName("Milan Mitra");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("AdminPassword123"));
            admin.setRole(RoleEnum.ADMIN);
            admin.setActive(true);
            admin.setProvider(ProviderEnum.MANUAL);
            userRepository.save(admin);
            log.info("✅ Admin user seeded successfully!");
        } else {
            log.info("ℹ️ Admin user already exists. Skipping seeding.");
        }
    }
}
