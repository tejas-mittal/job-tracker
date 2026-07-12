package com.jobtracker.monolith.auth.controller;

import com.jobtracker.monolith.auth.dto.RefreshRequest;
import com.jobtracker.monolith.auth.dto.TokenResponse;
import com.jobtracker.monolith.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public auth endpoints available in all profiles.
 *
 * <p>Endpoint summary:
 * <ul>
 *   <li>{@code GET  /auth/authorize/google} â€” redirects to Google consent screen
 *       (handled automatically by Spring Security OAuth2 login, not this controller).</li>
 *   <li>{@code POST /auth/refresh} â€” exchanges a refresh token for a new token pair.</li>
 *   <li>{@code POST /auth/logout} â€” revokes all refresh tokens for the user.</li>
 * </ul>
 *
 * <p>The Google login initiation and callback are handled by Spring Security's
 * built-in OAuth2 login machinery, configured in {@link com.jobtracker.monolith.auth.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/refresh
     * Exchanges a (single-use) refresh token for a fresh access + refresh token pair.
     * The old refresh token is immediately revoked after successful use.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * POST /auth/logout
     * Revokes all refresh tokens for the user (logs out every device/session).
     * Requires the caller to supply their user ID via {@code X-User-Id} header
     * (populated by the API gateway from the validated JWT).
     *
     * <p>Note: because JWTs are stateless, existing access tokens remain valid
     * until they expire naturally (~15 min). To fully invalidate access tokens,
     * a token blacklist or shorter expiry would be needed â€” an accepted trade-off
     * for this architecture.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-User-Id") UUID userId) {
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
