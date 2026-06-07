package at.htlwels.votevox.auth;

import at.htlwels.votevox.audit.AuditService;
import at.htlwels.votevox.auth.dto.LoginRequest;
import at.htlwels.votevox.auth.dto.LoginResponse;
import at.htlwels.votevox.common.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates admin users by email + password and issues JWTs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown email: {}", request.email());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            auditService.logAdmin(admin.getId(), "LOGIN_FAILED",
                    "Email: " + admin.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        String jwt = jwtTokenProvider.generateToken(admin);
        auditService.logAdmin(admin.getId(), "LOGIN_SUCCESS",
                "Email: " + admin.getEmail());
        log.info("Admin {} logged in successfully", admin.getEmail());
        return new LoginResponse(jwt, admin.getRole(), admin.getName());
    }
}
