package com.jobtracker.monolith.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsResponse(
        UUID userId,
        int totalApplications,
        int activeApplications,
        int rejectedApplications,
        int offersReceived,
        int funnelApplied,
        int funnelInterview,
        int funnelOffer,
        Double avgDaysToOffer,
        Instant updatedAt
) {}
