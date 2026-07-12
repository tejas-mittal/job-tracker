package com.jobtracker.monolith.tracker.dto;

import com.jobtracker.monolith.tracker.entity.ApplicationSource;
import com.jobtracker.monolith.tracker.entity.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound DTO returned by all application endpoints.
 *
 * <p>Using a Java record: immutable, compact, no boilerplate.
 * MapStruct generates the constructor call automatically.
 */
public record ApplicationResponse(
        UUID id,
        UUID userId,
        String company,
        String role,
        ApplicationStatus status,
        ApplicationSource source,
        LocalDate appliedDate,
        Instant lastUpdatedAt,
        String notes,
        String interviewLink,
        String interviewTime,
        String assessmentDate,
        String sourceEmailAddress,
        Boolean isArchived
) {}
