package io.ngrabner.task_tracker_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final FixedWindowRateLimiter limiter;

    public AuthRateLimitFilter(FixedWindowRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate limit the Google auth exchange endpoint
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/google".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = clientIp(request);

        if (!limiter.allow(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many login attempts. Try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // In AWS behind a proxy/ALB you’ll want X-Forwarded-For
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
