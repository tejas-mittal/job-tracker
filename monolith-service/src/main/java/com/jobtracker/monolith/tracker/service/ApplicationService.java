package com.jobtracker.monolith.tracker.service;

import com.jobtracker.monolith.tracker.dto.ApplicationFilter;
import com.jobtracker.monolith.tracker.dto.ApplicationResponse;
import com.jobtracker.monolith.tracker.dto.CreateApplicationRequest;
import com.jobtracker.monolith.tracker.dto.UpdateApplicationRequest;
import com.jobtracker.monolith.events.EmailStatusDetectedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Application use-case interface.
 *
 * <p>Controllers and RabbitMQ listeners depend ONLY on this interface,
 * never on the concrete implementation. This enables:
 * <ul>
 *   <li>Independent unit testing of the service implementation with mocked dependencies</li>
 *   <li>Swappable implementations without touching calling code</li>
 * </ul>
 *
 * <p>All business logic, state-transition validation, and event publishing
 * lives in {@link com.jobtracker.monolith.tracker.service.impl.ApplicationServiceImpl}.
 */
public interface ApplicationService {

    /**
     * Creates a new job application for the given user.
     * Publishes an {@code application.created} event on success.
     *
     * @param userId  extracted from JWT sub claim (or X-User-Id header in local profile)
     * @param request validated create payload
     * @return the persisted application as a response DTO
     */
    ApplicationResponse createApplication(UUID userId, CreateApplicationRequest request);

    /**
     * Returns all applications for the user, optionally filtered by status and company.
     * Results are ordered by lastUpdatedAt descending.
     */
    List<ApplicationResponse> listApplications(UUID userId, ApplicationFilter filter);

    /**
     * Returns a single application. Throws ApplicationNotFoundException if
     * not found or if the application belongs to a different user.
     */
    ApplicationResponse getApplication(UUID userId, UUID applicationId);

    /**
     * Applies a partial update (status and/or notes) to an existing application.
     * Validates manual state-transition rules before persisting.
     * Publishes a {@code status.changed} event when the status field changes.
     *
     * @throws com.jobtracker.monolith.tracker.exception.InvalidStatusTransitionException on illegal transition
     * @throws com.jobtracker.monolith.tracker.exception.ApplicationNotFoundException     if not found / wrong user
     */
    ApplicationResponse updateApplication(UUID userId, UUID applicationId, UpdateApplicationRequest request);

    /**
     * Permanently deletes an application.
     *
     * @throws com.jobtracker.monolith.tracker.exception.ApplicationNotFoundException if not found / wrong user
     */
    void deleteApplication(UUID userId, UUID applicationId);

    /**
     * Handles an inbound {@code email.status_detected} event from email-service.
     *
     * <p>Logic:
     * <ol>
     *   <li>Idempotency check â€” if eventId already processed, return immediately.</li>
     *   <li>Attempt to find a matching application (company + role, or company-only fallback).</li>
     *   <li>If match found and not in terminal state, update status and publish {@code status.changed}.</li>
     *   <li>If no match found, create a new application and publish both
     *       {@code application.created} and {@code status.changed}.</li>
     *   <li>Record the eventId in processed_events regardless of outcome.</li>
     * </ol>
     *
     * <p>This method is called by the RabbitMQ listener â€” all business logic
     * must remain here, not in the listener class.
     */
    void handleEmailStatusDetected(EmailStatusDetectedEvent event);
}
