package at.htlwels.votevox.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads the {@code Authorization: Bearer ...} header, validates the JWT
 * via {@link JwtTokenProvider}, and populates the SecurityContext with an
 * {@link AuthenticatedPrincipal}.
 * <p>If no token is present or it is invalid the request continues
 * unauthenticated; downstream security rules decide whether to reject it.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String jwt = header.substring(BEARER_PREFIX.length()).trim();
            Claims claims = tokenProvider.parse(jwt);
            if (claims != null) {
                String email = tokenProvider.extractSubject(claims);
                AdminRole role = tokenProvider.extractRole(claims);
                UUID userId = tokenProvider.extractUserId(claims);

                if (email != null && role != null) {
                    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, email, role);
                    var authority = new SimpleGrantedAuthority("ROLE_" + role.name());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(authority));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Authenticated admin principal exposed via {@code Authentication#getPrincipal()}.
     */
    public record AuthenticatedPrincipal(UUID userId, String email, AdminRole role) {}
}
