package com.jobtracker.monolith.email.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GmailAlreadyLinkedException extends ResponseStatusException {
    public GmailAlreadyLinkedException(String gmailAddress) {
        super(HttpStatus.CONFLICT, "Gmail account already linked: " + gmailAddress);
    }
}
