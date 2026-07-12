package com.jobtracker.monolith.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing a single job application owned by one user.
 *
 * <p>Controllers never reference this class directly â€” they work with DTOs.
 * Entity â†” DTO mapping lives in {@link com.jobtracker.monolith.tracker.mapper.ApplicationMapper}.
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Owner â€” sourced from the JWT sub claim; never changes after creation. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "role")
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ApplicationSource source;

    @Column(name = "applied_date")
    private LocalDate appliedDate;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;


    @Column(name = "source_email_address")
    private String sourceEmailAddress;

    @Column(name = "interview_link", length = 500)
    private String interviewLink;

    @Column(name = "interview_time")
    private String interviewTime;

    @Column(name = "assessment_date")
    private String assessmentDate;


    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PrePersist
    protected void onCreate() {
        if (lastUpdatedAt == null) {
            lastUpdatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
