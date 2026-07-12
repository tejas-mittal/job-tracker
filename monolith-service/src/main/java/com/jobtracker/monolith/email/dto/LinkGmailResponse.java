package com.jobtracker.monolith.email.dto;

/** Response for step 1 of Gmail linking â€” contains the Google authorization URL. */
public record LinkGmailResponse(
        String authorizationUrl,
        String instructions
) {}
