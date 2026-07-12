package com.jobtracker.monolith.auth.controller;

import com.jobtracker.monolith.auth.dto.DevTokenRequest;
import com.jobtracker.monolith.auth.dto.TokenResponse;
import com.jobtracker.monolith.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local-profile-only controller for testing without Google OAuth2.
 *
 * <p>Activated only when {@code spring.profiles.active=local}.
 * This endpoint calls {@link AuthService#loginOrRegister} directly,
 * bypassing the OAuth2 redirect flow entirely.
 *
 * <p>Usage:
 * <pre>
 * POST /auth/dev-token
 * Content-Type: application/json
 *
 * { "googleId": "test-google-123", "email": "dev@example.com", "displayName": "Dev User" }
 * </pre>
 *
 * Returns a real JWT that can be used as a Bearer token in all services.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class DevTokenController {

    private final AuthService authService;

    @PostMapping("/dev-token")
    public ResponseEntity<TokenResponse> devToken(
            @Valid @RequestBody DevTokenRequest request) {

        TokenResponse tokens = authService.loginOrRegister(
                request.googleId(),
                request.email(),
                request.displayName(),
                null
        );

        return ResponseEntity.ok(tokens);
    }
}
