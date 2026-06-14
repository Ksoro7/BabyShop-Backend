package OCI.BabyShop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de rate limiting pour les endpoints d'authentification.
 * Implémentation en mémoire (ConcurrentHashMap), sans Redis.
 * SÉCURITÉ : Protège contre les attaques par force brute sur l'authentification.
 *
 * Règles :
 * - POST /api/auth/login        : max 5 tentatives / 15 min par IP
 * - POST /api/auth/register     : max 3 tentatives / 1h par IP
 * - POST /api/auth/forgot-password : max 3 tentatives / 1h par IP
 *
 * @change [PROD-READY] Rate limiting sur les endpoints auth - 2026-06-12
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    private static final long LOGIN_WINDOW_MS = 15 * 60 * 1000L;
    private static final int LOGIN_MAX_ATTEMPTS = 5;

    private static final long STRICT_WINDOW_MS = 60 * 60 * 1000L;
    private static final int STRICT_MAX_ATTEMPTS = 3;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        int maxAttempts;
        long windowMs;

        if (path.endsWith("/api/auth/login")) {
            maxAttempts = LOGIN_MAX_ATTEMPTS;
            windowMs = LOGIN_WINDOW_MS;
        } else if (path.endsWith("/api/auth/register") || path.endsWith("/api/auth/forgot-password")) {
            maxAttempts = STRICT_MAX_ATTEMPTS;
            windowMs = STRICT_WINDOW_MS;
        } else {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        String key = ip + ":" + path;
        long now = System.currentTimeMillis();

        // SÉCURITÉ : compute est atomique (ConcurrentHashMap) — pas de race condition
        RateLimitEntry entry = attempts.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > windowMs) {
                return new RateLimitEntry(1, now);
            }
            return new RateLimitEntry(existing.count + 1, existing.windowStart);
        });

        if (entry.count > maxAttempts) {
            long retryAfterMs = windowMs - (now - entry.windowStart);
            long retryAfterMinutes = Math.max(1, retryAfterMs / 60_000);
            log.warn("Rate limit atteint pour {} ({} tentatives sur {})", key, entry.count - 1, maxAttempts);
            response.setStatus(429);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(
                    "{\"erreur\":\"Trop de tentatives. R\u00e9essayez dans " + retryAfterMinutes + " minutes.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitEntry(int count, long windowStart) {}
}
