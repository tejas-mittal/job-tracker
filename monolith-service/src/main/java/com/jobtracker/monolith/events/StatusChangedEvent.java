package com.jobtracker.monolith.events;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound event published when an application's status changes,
 * whether triggered manually or via auto-detected email.
 *
 * <p>Routing key: {@code status.changed}
 * <p>Consumed by: notification-service, analytics-service.
 */
@Data
@Builder
public class StatusChangedEvent {

    /** Unique event ID â€” consumers use this for idempotency checks. */
    private UUID eventId;

    private String eventType;   // always "status.changed"

    private UUID userId;
    private Instant timestamp;

    // â”€â”€ Application payload â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private UUID applicationId;
    private String company;
    private String role;

    /** May be null for brand-new auto-created applications (first status is also new status). */
    private String previousStatus;

    private String newStatus;

    /** "MANUAL" or "AUTO" â€” lets consumers know what triggered the change. */
    private String source;
}
