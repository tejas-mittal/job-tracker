package com.jobtracker.monolith.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Event published to RabbitMQ when an email is classified as a job application signal.
 *
 * <p>Must be compatible with the {@code EmailStatusDetectedEvent} POJO consumed
 * by tracker-service's {@code EmailStatusDetectedListener}.
 *
 * <p>Field names use camelCase (Jackson serialises to JSON by default).
 */
@Data
@Builder
public class EmailStatusDetectedEvent {

    /** Idempotency key â€” tracker-service deduplicates on this. */
    private UUID eventId;

    private String eventType;

    /** The user who owns the linked Gmail account. */
    private UUID userId;
    
    private java.time.Instant timestamp;

    /** Company extracted from email sender domain or content (best-effort). */
    private String company;

    /** Role/job title extracted from subject (may be null). */
    private String role;

    /**
     * The classified status as a string matching tracker-service's
     * {@code ApplicationStatus} enum values: INTERVIEW, REJECTED, OFFER, WITHDRAWN.
     */
    private String detectedStatus;

    /** Gmail message ID â€” for cross-service traceability. */
    private String sourceEmailId;

    /** The Gmail address that received this email (to segregate applications by linked inbox). */
    private String sourceEmailAddress;
    
    private double confidence;

    private String interviewLink;

    private String interviewTime;

    private String assessmentDate;

    private String notes;
}
