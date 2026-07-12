package com.jobtracker.monolith.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/dev-token (local profile only).
 * Simulates a Google login without the OAuth2 redirect flow.
 */
public record DevTokenRequest(
        @NotBlank String googleId,
        @Email @NotBlank String email,
        String displayName
) {}
