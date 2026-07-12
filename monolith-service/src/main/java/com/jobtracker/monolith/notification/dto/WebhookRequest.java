package com.jobtracker.monolith.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebhookRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 255)            String secret  // optional HMAC signing secret
) {}
