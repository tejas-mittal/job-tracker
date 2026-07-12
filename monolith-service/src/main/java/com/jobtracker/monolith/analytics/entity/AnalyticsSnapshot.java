package com.jobtracker.monolith.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSnapshot {
    @Id
    private UUID userId;

    private int totalApplications = 0;
    private int activeApplications = 0;
    private int rejectedApplications = 0;
    private int offersReceived = 0;

    private int funnelApplied = 0;
    private int funnelInterview = 0;
    private int funnelOffer = 0;

    private Double avgDaysToOffer;

    private Instant updatedAt = Instant.now();

    public AnalyticsSnapshot(UUID userId) {
        this.userId = userId;
    }
}
