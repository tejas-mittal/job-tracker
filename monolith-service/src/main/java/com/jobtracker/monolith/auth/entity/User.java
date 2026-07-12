package com.jobtracker.monolith.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a registered user who has completed at least one Google OAuth2 login.
 *
 * <p>Rows are created on first login and updated (last_login_at) on subsequent logins.
 * The {@code googleId} (Google's stable {@code sub} claim) is the natural key.
 * Our internal {@code id} (UUID) is what gets embedded in JWTs as the {@code sub} claim
 * â€” this decouples internal identity from the Google-specific identifier.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Google's stable user identifier (the {@code sub} field in Google's ID token). */
    @Column(name = "google_id", nullable = false, unique = true, length = 255)
    private String googleId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "picture_url", length = 1024)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null)   createdAt   = now;
        if (lastLoginAt == null) lastLoginAt = now;
    }
}
