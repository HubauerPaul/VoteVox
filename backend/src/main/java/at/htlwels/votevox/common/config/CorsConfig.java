package at.htlwels.votevox.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Centralized CORS configuration. Allowed origin <em>patterns</em> are loaded
 * from {@code votevox.cors.allowed-origins} (see application.yml).
 * <p>
 * Patterns (not exact origins) are used so the dev setup works regardless of
 * scheme (http/https - the UIs run on HTTPS via mkcert) and the machine's LAN
 * IP/port. The defaults permit localhost and private LAN ranges on any port.
 * {@code setAllowedOriginPatterns} is required because exact-origin matching
 * cannot be combined with wildcards while {@code allowCredentials} is true.
 * </p>
 */
@Configuration
public class CorsConfig {

    @Value("${votevox.cors.allowed-origins:"
            + "http://localhost:*,https://localhost:*,"
            + "http://127.0.0.1:*,https://127.0.0.1:*,"
            + "http://192.168.*:*,https://192.168.*:*,"
            + "http://10.*:*,https://10.*:*,"
            + "http://172.*:*,https://172.*:*}")
    private List<String> allowedOrigins;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
