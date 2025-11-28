package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:admin@hamradio.local}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        // Only create admin if username and password are provided
        if (adminUsername == null || adminUsername.isEmpty() ||
                adminPassword == null || adminPassword.isEmpty()) {
            log.info("Admin credentials not configured in environment variables. Skipping admin user creation.");
            return;
        }

        // Check if admin user already exists
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user '{}' already exists. Skipping creation.", adminUsername);
            return;
        }

        // Create admin user
        User adminUser = new User();
        adminUser.setUsername(adminUsername);
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFullName("System Administrator");
        adminUser.setEnabled(true);
        adminUser.setAccountNonExpired(true);
        adminUser.setAccountNonLocked(true);
        adminUser.setCredentialsNonExpired(true);

        // Assign admin and user roles
        Set<User.Role> roles = new HashSet<>();
        roles.add(User.Role.ROLE_ADMIN);
        roles.add(User.Role.ROLE_USER);
        adminUser.setRoles(roles);

        // Save admin user
        userRepository.save(adminUser);

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  ADMIN USER CREATED SUCCESSFULLY                         ║");
        log.info("║  Username: {}                                   ║", String.format("%-44s", adminUsername));
        log.info("║  Email: {}                          ║", String.format("%-47s", adminEmail));
        log.info("║  Roles: ROLE_ADMIN, ROLE_USER                            ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
    }
}
