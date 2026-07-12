package com.jobtracker.monolith.auth.service;

import com.jobtracker.monolith.auth.config.JwtUtil;
import com.jobtracker.monolith.auth.dto.TokenResponse;
import com.jobtracker.monolith.auth.entity.RefreshToken;
import com.jobtracker.monolith.auth.entity.User;
import com.jobtracker.monolith.auth.exception.InvalidRefreshTokenException;
import com.jobtracker.monolith.auth.repository.RefreshTokenRepository;
import com.jobtracker.monolith.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication business logic.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Upsert users on Google OAuth2 login (create on first visit, update last_login_at on return).</li>
 *   <li>Issue access + refresh token pairs.</li>
 *   <li>Rotate refresh tokens on refresh â€” each token is single-use.</li>
 *   <li>Detect token-family compromise (replay of a revoked token) and revoke all tokens for the user.</li>
 *   <li>Revoke all tokens on logout.</li>
 * </ol>
 *
 * <p>The raw refresh token is generated here and returned exactly once.
 * Only its SHA-256 hash is persisted â€” the plaintext is never stored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil                jwtUtil;

    @Value("${security.jwt.refresh-token-expiry-seconds}")
    private long refreshTokenExpirySeconds;

    private static final SecureRandom RANDOM = new SecureRandom();

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // OAuth2 Login (called by OAuth2SuccessHandler)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Processes a successful Google OAuth2 login.
     * Creates the user on first visit; updates {@code lastLoginAt} on return.
     * Always issues a fresh token pair.
     *
     * @param googleId    Google's stable {@code sub} claim
     * @param email       user's email from Google
     * @param displayName display name (may be null)
     * @param pictureUrl  profile photo URL (may be null)
     * @return a fresh access + refresh token pair
     */
    @Transactional
    public TokenResponse loginOrRegister(String googleId, String email,
                                          String displayName, String pictureUrl) {
        User user = userRepository.findByGoogleId(googleId)
                .map(existing -> {
                    existing.setLastLoginAt(Instant.now());
                    existing.setDisplayName(displayName);
                    existing.setPictureUrl(pictureUrl);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    log.info("New user registered: googleId={} email={}", googleId, email);
                    return userRepository.save(User.builder()
                            .googleId(googleId)
                            .email(email)
                            .displayName(displayName)
                            .pictureUrl(pictureUrl)
                            .build());
                });

        return issueTokenPair(user);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Token Refresh
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Rotates the refresh token:
     * <ol>
     *   <li>Hash the presented raw token and look it up.</li>
     *   <li>If not found â†’ reject (unknown token).</li>
     *   <li>If found but revoked â†’ token-family compromise: revoke all user tokens.</li>
     *   <li>If expired â†’ reject.</li>
     *   <li>Otherwise â†’ revoke the old token, issue a fresh pair.</li>
     * </ol>
     *
     * @param rawRefreshToken the raw token string presented by the client
     * @return a fresh token pair
     */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(hash);

        if (tokenOpt.isEmpty()) {
            log.warn("Refresh attempted with unknown token hash");
            throw new InvalidRefreshTokenException("token not found");
        }

        RefreshToken storedToken = tokenOpt.get();

        // â”€â”€ Token-family compromise detection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (storedToken.isRevoked()) {
            UUID compromisedUserId = storedToken.getUser().getId();
            log.warn("Revoked refresh token replayed for userId={} â€” revoking all tokens (family compromise)",
                     compromisedUserId);
            refreshTokenRepository.revokeAllForUser(compromisedUserId);
            throw new InvalidRefreshTokenException("token already revoked â€” all sessions have been terminated");
        }

        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidRefreshTokenException("token expired");
        }

        // â”€â”€ Rotate: revoke old, issue new â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        User user = storedToken.getUser();
        TokenResponse newTokens = issueTokenPair(user);

        // Mark the old token as revoked and chain it to the new one
        // (we can look up the new token's ID from the hash of the new refresh token)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        log.info("Refreshed token pair for userId={}", user.getId());
        return newTokens;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Logout
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Revokes all refresh tokens for the user (logs out all sessions).
     */
    @Transactional
    public void logout(UUID userId) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId);
        log.info("Logged out userId={} â€” {} token(s) revoked", userId, revoked);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Private helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Generates access + refresh token pair and persists the refresh token hash.
     */
    private TokenResponse issueTokenPair(User user) {
        // Access token (JWT)
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());

        // Refresh token â€” random 256-bit value, base64url-encoded
        String rawRefreshToken = generateSecureToken();
        String tokenHash       = sha256Hex(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirySeconds))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(accessToken, jwtUtil.getAccessTokenExpirySeconds(), rawRefreshToken);
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Returns hex-encoded SHA-256 hash of the input string.
     * Uses UTF-8 encoding. SHA-256 cannot be constructed without the algorithm
     * available â€” wrapped in an unchecked exception since this is always present in the JVM.
     */
    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
