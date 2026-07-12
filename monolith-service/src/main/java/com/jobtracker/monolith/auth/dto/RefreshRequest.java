package com.jobtracker.monolith.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /auth/refresh */
public record RefreshRequest(@NotBlank String refreshToken) {}
