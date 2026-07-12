package com.jobtracker.monolith.email.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class GmailAccountNotFoundException extends ResponseStatusException {
    public GmailAccountNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Gmail account not found: " + id);
    }
}
