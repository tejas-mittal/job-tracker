package com.jobtracker.monolith.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class NotificationNotFoundException extends ResponseStatusException {
    public NotificationNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Notification not found: " + id);
    }
}
