package com.jobtracker.monolith.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookResponse(UUID id, String url, boolean active, Instant createdAt) {}
