package com.jobtracker.monolith.tracker.dto;

import com.jobtracker.monolith.tracker.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * Request body for PATCH /applications/{id}.
 *
 * <p>All fields are optional â€” only non-null fields are applied.
 * Callers can update status, notes, or both in a single request.
 */
@Data
@Builder
public class UpdateApplicationRequest {

    /**
     * Target status. Must be a valid manual transition from the current status.
     * Terminal states (REJECTED, WITHDRAWN) may not transition further.
     */
    private ApplicationStatus status;

    /** Free-text notes; {@code null} means "leave unchanged". */
    private String notes;

    private String company;

    private String role;

    private LocalDate appliedDate;

    private String interviewTime;

    private String assessmentDate;

    private String interviewLink;

    private Boolean isArchived;
}
