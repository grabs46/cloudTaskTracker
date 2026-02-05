package io.ngrabner.task_tracker_api.config;

import io.ngrabner.task_tracker_api.auth.JwtCookieAuthFilter;
import io.ngrabner.task_tracker_api.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.ngrabner.task_tracker_api.security.AuthRateLimitFilter;
import io.ngrabner.task_tracker_api.security.FixedWindowRateLimiter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final String cookieName;

    @Value("${app.security.auth-rate-limit.window-seconds:60}")
    private long rlWindowSeconds;

    @Value("${app.security.auth-rate-limit.max-requests:10}")
    private int rlMaxRequests;

    public SecurityConfig(
            JwtService jwtService,
            @Value("${app.jwt.cookie-name:tt_access}") String cookieName) {
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // v1: OK; revisit CSRF later since you use cookies
                .cors(cors -> {
                }) // Step 13 will configure allowed origins properly
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers("/docs/**", "/api-docs/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // protected
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers("/api/tasks/**").authenticated()

                        .anyRequest().permitAll())

                .addFilterBefore(
                        new AuthRateLimitFilter(new FixedWindowRateLimiter(rlWindowSeconds, rlMaxRequests)),
                        UsernamePasswordAuthenticationFilter.class)

                // Install JWT cookie filter
                .addFilterBefore(new JwtCookieAuthFilter(jwtService, cookieName),
                        UsernamePasswordAuthenticationFilter.class)

                // Prevent browser basic-auth popup + login redirects
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401)));

        return http.build();
    }
}
