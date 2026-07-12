package com.jobtracker.monolith.tracker.dto;

import java.time.Instant;

/** Standard error envelope returned by GlobalExceptionHandler for all 4xx/5xx responses. */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {}
