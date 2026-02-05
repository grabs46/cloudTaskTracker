package io.ngrabner.task_tracker_api.auth;

import io.jsonwebtoken.Claims;
import io.ngrabner.task_tracker_api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String cookieName;

    public JwtCookieAuthFilter(JwtService jwtService, String cookieName) {
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // If already authenticated, skip
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = readCookie(request, cookieName);
                if (token != null && !token.isBlank()) {
                    Claims claims = jwtService.parseClaims(token);

                    Long userId = Long.valueOf(claims.getSubject());
                    String email = claims.get("email", String.class);

                    CurrentUser principal = new CurrentUser(userId, email);

                    // No roles for v1 -> empty authorities list
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // Invalid/expired token -> treat as unauthenticated
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()))
                return c.getValue();
        }
        return null;
    }
}
