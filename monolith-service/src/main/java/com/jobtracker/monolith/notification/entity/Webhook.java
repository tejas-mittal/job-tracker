package com.jobtracker.monolith.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** A user-registered webhook endpoint for outbound notification delivery. */
@Entity
@Table(name = "webhooks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    /** Optional HMAC-SHA256 signing secret â€” included as X-Signature-256 header. */
    @Column(name = "secret", length = 255)
    private String secret;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (!active) active = true;
    }
}
