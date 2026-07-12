package com.jobtracker.monolith.tracker.controller;

import com.jobtracker.monolith.tracker.dto.ErrorResponse;
import com.jobtracker.monolith.tracker.exception.ApplicationNotFoundException;
import com.jobtracker.monolith.tracker.exception.InvalidStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler that translates domain exceptions into consistent
 * {@link ErrorResponse} JSON bodies.
 *
 * <p>Error contract: all error responses share the same shape so API clients
 * have a single parsing path.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 â€” application not found or wrong owner. */
    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ApplicationNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    /** 422 â€” manual status transition that violates the allowed-transition table. */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI());
    }

    /** 400 â€” @Valid annotation failures on request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, req.getRequestURI());
    }

    /** 400 â€” malformed JSON or unrecognised enum values in request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Malformed request body: " + ex.getMostSpecificCause().getMessage(),
                     req.getRequestURI());
    }

    /** Propagate ResponseStatusException directly (used by argument resolvers). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        return error(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), req.getRequestURI());
    }

    /** 500 â€” catch-all for unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                     "An unexpected error occurred. Please try again later.",
                     req.getRequestURI());
    }

    // â”€â”€ Helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                Instant.now()
        ));
    }
}
