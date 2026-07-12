package com.jobtracker.monolith.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

import com.jobtracker.monolith.tracker.entity.ApplicationStatus;

/** Request body for POST /applications. */
@Data
@Builder
public class CreateApplicationRequest {
    private ApplicationStatus status;

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String company;

    @Size(max = 255, message = "Role must not exceed 255 characters")
    private String role;

    /** Defaults to today in the service layer if not provided. */
    private LocalDate appliedDate;

    private String notes;

    @Builder.Default
    private boolean isArchived = false;

    private String interviewTime;

    private String assessmentDate;

    private String interviewLink;
}
