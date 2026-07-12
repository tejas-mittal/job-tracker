package com.jobtracker.monolith.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when a presented refresh token is invalid, expired, or already revoked. */
public class InvalidRefreshTokenException extends ResponseStatusException {
    public InvalidRefreshTokenException(String reason) {
        super(HttpStatus.UNAUTHORIZED, "Invalid refresh token: " + reason);
    }
}
