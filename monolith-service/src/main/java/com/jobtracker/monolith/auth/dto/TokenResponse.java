package com.jobtracker.monolith.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token pair returned to the client after successful login or token refresh.
 *
 * <p>The access token is a short-lived JWT (default 15 min).
 * The refresh token is an opaque random string (default 30 days).
 * Both are returned in the body; clients should store the refresh token
 * securely (e.g. HttpOnly cookie) and discard it after use.
 */
public record TokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("expires_in")    long   expiresIn,    // seconds
        @JsonProperty("refresh_token") String refreshToken
) {
    public static TokenResponse of(String accessToken, long expiresIn, String refreshToken) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, refreshToken);
    }
}
