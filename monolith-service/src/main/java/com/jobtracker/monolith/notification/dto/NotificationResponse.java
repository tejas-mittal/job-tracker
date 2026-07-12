package com.jobtracker.monolith.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID    id,
        UUID    eventId,
        String  type,
        String  title,
        String  body,
        boolean read,
        Instant createdAt
) {}
