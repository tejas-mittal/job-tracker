package com.jobtracker.monolith.tracker.exception;

import java.util.UUID;

/** Thrown when an application is not found or does not belong to the requesting user. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID applicationId) {
        super("Application not found: " + applicationId);
    }
}
