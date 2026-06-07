package at.htlwels.votevox.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * On application startup, replaces the {@code BCRYPT_PLACEHOLDER} hash seeded
 * by Flyway V2 with a freshly computed BCrypt hash for the default admin
 * password. The credentials are logged ONCE so the first start can be
 * recovered; in production, the password MUST be rotated immediately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private static final String PLACEHOLDER = "BCRYPT_PLACEHOLDER";
    private static final String DEFAULT_PASSWORD = "Admin1234!";
    private static final String DEFAULT_EMAIL = "admin@votevox.at";

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeAdminPasswords() {
        List<AdminUser> placeholders = adminUserRepository.findAllByPasswordHash(PLACEHOLDER);
        if (placeholders.isEmpty()) {
            log.debug("No placeholder admin hashes found - skipping initialization");
            return;
        }

        // Prefix with {bcrypt} so the DelegatingPasswordEncoder can identify the algorithm.
        String encoded = "{bcrypt}" + bcryptPasswordEncoder.encode(DEFAULT_PASSWORD);
        for (AdminUser admin : placeholders) {
            admin.setPasswordHash(encoded);
            adminUserRepository.save(admin);
            log.warn("=====================================================================");
            log.warn("Default admin credentials initialized for first run:");
            log.warn("  Email:    {}", admin.getEmail() != null ? admin.getEmail() : DEFAULT_EMAIL);
            log.warn("  Password: {}", DEFAULT_PASSWORD);
            log.warn("  Role:     {}", admin.getRole());
            log.warn("PLEASE ROTATE THIS PASSWORD IMMEDIATELY.");
            log.warn("=====================================================================");
        }
    }
}
