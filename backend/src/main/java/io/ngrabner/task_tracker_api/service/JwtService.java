package io.ngrabner.task_tracker_api.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long minutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.minutes:15}") long minutes) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.minutes = minutes;
    }

    public String createToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(minutes * 60);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public io.jsonwebtoken.Claims parseClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
