package com.jobtracker.monolith.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Low-level JWT creation and validation using JJWT 0.12.x.
 *
 * <p>Algorithm: HS256 (HMAC-SHA256) with a shared secret.
 * The same secret must be configured in all resource servers
 * (tracker-service, api-gateway, etc.) that validate these tokens.
 *
 * <p>JWT claims:
 * <ul>
 *   <li>{@code sub} â€” user's internal UUID (NOT the Google sub)</li>
 *   <li>{@code email} â€” user's email address</li>
 *   <li>{@code iat} â€” issued-at (epoch seconds)</li>
 *   <li>{@code exp} â€” expiry (epoch seconds)</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpirySeconds;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiry-seconds}") long accessTokenExpirySeconds) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
    }

    /**
     * Creates a signed HS256 access token.
     *
     * @param userId   the user's internal UUID â€” becomes the {@code sub} claim
     * @param email    the user's email â€” included as a custom claim
     * @return compact JWT string
     */
    public String generateAccessToken(UUID userId, String email) {
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirySeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates the token signature and expiry.
     *
     * @param token compact JWT string
     * @return parsed Claims if valid
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the {@code sub} claim without full validation.
     * Use only after {@link #validateAndExtractClaims} has already succeeded.
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }
}
