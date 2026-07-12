package com.jobtracker.monolith.tracker.dto;

import com.jobtracker.monolith.tracker.entity.ApplicationStatus;

/**
 * Query parameters for GET /applications.
 * Both fields are optional; null means "no filter on this dimension".
 */
public record ApplicationFilter(

        /** Filter by exact status. Null = all statuses. */
        ApplicationStatus status,

        /** Case-insensitive substring match on company name. Null = all companies. */
        String company,

        /** Filter by archived status. Null = include all. */
        Boolean archived
) {}
