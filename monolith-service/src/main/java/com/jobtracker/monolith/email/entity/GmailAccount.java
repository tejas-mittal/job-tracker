package com.jobtracker.monolith.email.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A Gmail account linked by a user for polling.
 *
 * <p><strong>Token security:</strong>
 * {@code accessTokenEnc} and {@code refreshTokenEnc} are stored encrypted with
 * AES-256-GCM. The format of each column is:
 * <pre>base64url( IV[12 bytes] || ciphertext || GCM-tag[16 bytes] )</pre>
 * Encryption/decryption is handled by {@link com.jobtracker.monolith.email.config.TokenEncryptionUtil}.
 *
 * <p>{@code historyId} is Google's Gmail API History ID â€” used for incremental
 * sync to avoid re-scanning the entire inbox on each poll.
 */
@Entity
@Table(name = "gmail_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Internal user ID (from auth-service JWT sub claim). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "gmail_address", nullable = false, length = 255)
    private String gmailAddress;

    /** AES-256-GCM encrypted Google access token. */
    @Column(name = "access_token_enc", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEnc;

    /** AES-256-GCM encrypted Google refresh token. */
    @Column(name = "refresh_token_enc", nullable = false, columnDefinition = "TEXT")
    private String refreshTokenEnc;

    /** UTC expiry time of the current access token. */
    @Column(name = "token_expiry")
    private Instant tokenExpiry;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    /**
     * Gmail History API cursor â€” used for incremental sync.
     * Null for brand-new accounts (triggers a full initial scan of recent messages).
     */
    @Column(name = "history_id", length = 64)
    private String historyId;

    @PrePersist
    void prePersist() {
        if (linkedAt == null) linkedAt = Instant.now();
    }

    public boolean isAccessTokenExpired() {
        return tokenExpiry != null && Instant.now().isAfter(tokenExpiry.minusSeconds(60));
    }
}
