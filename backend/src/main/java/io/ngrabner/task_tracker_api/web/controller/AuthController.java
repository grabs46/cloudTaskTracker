package io.ngrabner.task_tracker_api.web.controller;

import io.ngrabner.task_tracker_api.auth.GoogleTokenVerifierService;
import io.ngrabner.task_tracker_api.domain.User;
import io.ngrabner.task_tracker_api.repository.UserRepository;
import io.ngrabner.task_tracker_api.service.JwtService;
import io.ngrabner.task_tracker_api.web.dto.auth.GoogleAuthRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final String cookieName;
    private final boolean cookieSecure;
    private final long cookieMaxAgeSeconds;

    public AuthController(GoogleTokenVerifierService googleTokenVerifierService, UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.jwt.cookie-name:tt_access}") String cookieName,
            @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.jwt.minutes:15}") long minutes) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieMaxAgeSeconds = minutes * 60;
    }

    public record AuthResponse(Long userId, String email, String name) {
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            var info = googleTokenVerifierService.verify(request.getIdToken());

            User user = userRepository.findByGoogleSub(info.sub())
                    .orElseGet(User::new);

            user.setGoogleSub(info.sub());
            user.setEmail(info.email());
            user.setName(info.name());

            user = userRepository.save(user);

            String accessToken = jwtService.createToken(user.getId(), user.getEmail());

            ResponseCookie cookie = ResponseCookie.from(cookieName, accessToken)
                    .httpOnly(true)
                    .secure(cookieSecure) // false locally; true in prod (HTTPS)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(cookieMaxAgeSeconds)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AuthResponse(user.getId(), user.getEmail(), user.getName()));
        } catch (IllegalStateException e) {
            // backend misconfigured
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            // invalid token, wrong audience, expired, bad signature, etc.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
