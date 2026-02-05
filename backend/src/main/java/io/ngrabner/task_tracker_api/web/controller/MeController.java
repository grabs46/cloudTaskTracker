package io.ngrabner.task_tracker_api.web.controller;

import io.ngrabner.task_tracker_api.auth.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser cu)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(cu);
    }
}
