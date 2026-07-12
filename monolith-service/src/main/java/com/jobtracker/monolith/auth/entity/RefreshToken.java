package com.jobtracker.monolith.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted refresh token record.
 *
 * <p><strong>Security design:</strong>
 * <ul>
 *   <li>Only the SHA-256 hash ({@code tokenHash}) is stored â€” never the raw token.
 *       The raw token is generated, returned to the client once, and discarded.</li>
 *   <li>On each refresh, the old token is revoked and {@code replacedBy} is set to
 *       the new token's id, creating an auditable rotation chain.</li>
 *   <li>If a revoked token is presented again (replay attack or token theft),
 *       all tokens for that user are immediately revoked (token family compromise).</li>
 * </ul>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hex-encoded SHA-256 of the raw token string. Never the raw token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /**
     * Points to the token that replaced this one during rotation.
     * Null if this token has not been rotated yet.
     */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @PrePersist
    void prePersist() {
        if (issuedAt == null) issuedAt = Instant.now();
        if (!revoked)         revoked  = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
