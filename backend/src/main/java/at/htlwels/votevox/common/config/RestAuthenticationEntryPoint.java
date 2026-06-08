package at.htlwels.votevox.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns a JSON <strong>401 Unauthorized</strong> when a protected endpoint is
 * called without a valid token.
 * <p>
 * Without this, Spring Security's stateless default answers with 403 for
 * unauthenticated requests. The admin UI distinguishes the two: 401 means "your
 * session is gone, log in again" (and triggers a redirect to the login page),
 * whereas a stale token silently failing with 403 would leave the UI stuck.
 * </p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "Authentication required. Please log in again.");
        body.put("path", request.getRequestURI());
        body.put("fieldErrors", null);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
