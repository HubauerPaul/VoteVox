package at.htlwels.votevox.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Password encoder configuration.
 * <p>
 * Uses a {@link DelegatingPasswordEncoder} with BCrypt strength 12 as the
 * default. Stored hashes are prefixed with {@code {bcrypt}} so future
 * algorithm migrations stay backwards compatible. Plain BCrypt hashes
 * (without prefix) can still be validated through the same bean as a fallback.
 * </p>
 */
@Configuration
public class PasswordConfig {

    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(idForEncode, new BCryptPasswordEncoder(BCRYPT_STRENGTH));

        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder(idForEncode, encoders);
        // Fall back to BCrypt for legacy hashes without {bcrypt} prefix.
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder(BCRYPT_STRENGTH));
        return delegating;
    }

    /**
     * Direct BCrypt encoder for places where the prefix must NOT be added
     * (e.g. when generating the seeded admin password at startup).
     */
    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
