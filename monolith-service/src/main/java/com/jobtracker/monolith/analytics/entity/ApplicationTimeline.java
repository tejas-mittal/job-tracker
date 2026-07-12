package com.jobtracker.monolith.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_timeline", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"application_id", "status"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTimeline {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
