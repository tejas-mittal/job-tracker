package com.jobtracker.monolith.tracker.exception;

import com.jobtracker.monolith.tracker.entity.ApplicationStatus;

/** Thrown when a manual status transition violates the allowed-transition table. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super(String.format(
                "Cannot transition application from '%s' to '%s'. " +
                "Allowed transitions from '%s': %s",
                from, to, from, allowedFrom(from)
        ));
    }

    private static String allowedFrom(ApplicationStatus from) {
        return switch (from) {
            case APPLIED    -> "[ASSESSMENT, INTERVIEW, REJECTED, WITHDRAWN]";
            case ASSESSMENT -> "[INTERVIEW, OFFER, REJECTED, WITHDRAWN]";
            case INTERVIEW  -> "[OFFER, REJECTED, WITHDRAWN]";
            case OFFER      -> "[REJECTED, WITHDRAWN]";
            case REJECTED, WITHDRAWN -> "(none â€” terminal state)";
        };
    }
}
