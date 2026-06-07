package at.htlwels.votevox.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256-signed JSON Web Tokens used for admin
 * authentication. The token's subject is the admin's email; the role
 * is carried in the {@code role} claim, and the admin's UUID in {@code uid}.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "uid";

    @Value("${votevox.jwt.secret}")
    private String secret;

    @Value("${votevox.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("votevox.jwt.secret must be at least 32 bytes long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * @return a signed JWT carrying the admin's email, role and id.
     */
    public String generateToken(AdminUser admin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(admin.getEmail())
                .claim(CLAIM_ROLE, admin.getRole().name())
                .claim(CLAIM_USER_ID, admin.getId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the signature and expiration of the given JWT.
     * @return parsed claims or {@code null} if the token is invalid/expired.
     */
    public Claims parse(String jwt) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return null;
        }
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    public AdminRole extractRole(Claims claims) {
        Object role = claims.get(CLAIM_ROLE);
        return role == null ? null : AdminRole.valueOf(role.toString());
    }

    public UUID extractUserId(Claims claims) {
        Object uid = claims.get(CLAIM_USER_ID);
        return uid == null ? null : UUID.fromString(uid.toString());
    }
}
