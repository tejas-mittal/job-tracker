package com.jobtracker.monolith.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An in-app notification record.
 *
 * <p>{@code eventId} is the idempotency key sourced from the originating RabbitMQ event.
 * The UNIQUE constraint on {@code event_id} prevents double-inserting the same notification
 * if the message is redelivered.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Idempotency key â€” the eventId from the originating RabbitMQ event. */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    /** Notification type tag â€” e.g. STATUS_CHANGED, EMAIL_DETECTED. */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
