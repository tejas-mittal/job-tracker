package com.jobtracker.monolith.tracker.service.impl;

import com.jobtracker.monolith.tracker.dto.ApplicationFilter;
import com.jobtracker.monolith.tracker.dto.ApplicationResponse;
import com.jobtracker.monolith.tracker.dto.CreateApplicationRequest;
import com.jobtracker.monolith.tracker.dto.UpdateApplicationRequest;
import com.jobtracker.monolith.events.ApplicationCreatedEvent;
import com.jobtracker.monolith.events.EmailStatusDetectedEvent;
import com.jobtracker.monolith.events.StatusChangedEvent;
import com.jobtracker.monolith.tracker.entity.Application;
import com.jobtracker.monolith.tracker.entity.ApplicationSource;
import com.jobtracker.monolith.tracker.entity.ApplicationStatus;
import com.jobtracker.monolith.events.ProcessedEvent;
import com.jobtracker.monolith.tracker.exception.ApplicationNotFoundException;
import com.jobtracker.monolith.tracker.exception.InvalidStatusTransitionException;
import com.jobtracker.monolith.tracker.mapper.ApplicationMapper;
import com.jobtracker.monolith.tracker.messaging.EventPublisher;
import com.jobtracker.monolith.tracker.repository.ApplicationRepository;
import com.jobtracker.monolith.tracker.repository.ApplicationSpecification;
import com.jobtracker.monolith.tracker.repository.ProcessedEventRepository;
import com.jobtracker.monolith.tracker.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for job-application management.
 *
 * <p><strong>Architecture rules enforced here:</strong>
 * <ul>
 *   <li>Repositories are only accessed from this class â€” never from controllers or listeners.</li>
 *   <li>All state-transition validation lives here.</li>
 *   <li>Event publishing happens after the DB write is committed (within the same transaction).</li>
 *   <li>Entity â†” DTO conversion is delegated to {@link ApplicationMapper}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository    applicationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EventPublisher           eventPublisher;
    private final ApplicationMapper        applicationMapper;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // CREATE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public ApplicationResponse createApplication(UUID userId, CreateApplicationRequest request) {
        Application app = applicationMapper.toEntity(request);
        app.setUserId(userId);
        app.setStatus(request.getStatus() != null ? request.getStatus() : ApplicationStatus.APPLIED);
        app.setSource(ApplicationSource.MANUAL);
        app.setAppliedDate(request.getAppliedDate() != null ? request.getAppliedDate() : LocalDate.now());
        app.setLastUpdatedAt(Instant.now());
        if (app.getStatus() == ApplicationStatus.OFFER) {
            app.setIsArchived(true);
        }

        Application saved = applicationRepository.save(app);
        log.info("Created application id={} for userId={}", saved.getId(), userId);

        eventPublisher.publishApplicationCreated(buildCreatedEvent(saved));

        return applicationMapper.toResponse(saved);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // READ
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(UUID userId, ApplicationFilter filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "lastUpdatedAt");

        return applicationRepository
                .findAll(ApplicationSpecification.forUser(userId, filter.status(), filter.company(), filter.archived()), sort)
                .stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID userId, UUID applicationId) {
        return applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .map(applicationMapper::toResponse)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // UPDATE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public ApplicationResponse updateApplication(UUID userId, UUID applicationId,
                                                  UpdateApplicationRequest request) {
        Application app = applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        boolean dirty = false;
        ApplicationStatus previousStatus = null;

        // â”€â”€ Status update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (request.getStatus() != null && request.getStatus() != app.getStatus()) {
            ApplicationStatus current = app.getStatus();
            ApplicationStatus next    = request.getStatus();

            if (!current.canManuallyTransitionTo(next)) {
                throw new InvalidStatusTransitionException(current, next);
            }

            previousStatus = current;
            app.setStatus(next);
            if (next == ApplicationStatus.OFFER) {
                app.setIsArchived(true);
            }
            dirty = true;
        }

        // â”€â”€ Other fields update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (request.getNotes() != null) {
            app.setNotes(request.getNotes());
            dirty = true;
        }
        if (request.getCompany() != null) {
            app.setCompany(request.getCompany());
            dirty = true;
        }
        if (request.getRole() != null) {
            app.setRole(request.getRole());
            dirty = true;
        }
        if (request.getAppliedDate() != null) {
            app.setAppliedDate(request.getAppliedDate());
            dirty = true;
        }
        if (request.getInterviewTime() != null) {
            app.setInterviewTime(request.getInterviewTime());
            dirty = true;
        }
        if (request.getAssessmentDate() != null) {
            app.setAssessmentDate(request.getAssessmentDate());
            dirty = true;
        }
        if (request.getInterviewLink() != null) {
            app.setInterviewLink(request.getInterviewLink());
            dirty = true;
        }
        if (request.getIsArchived() != null) {
            app.setIsArchived(request.getIsArchived());
            dirty = true;
        }

        if (!dirty) {
            log.debug("PATCH /applications/{} had no changes â€” returning current state", applicationId);
            return applicationMapper.toResponse(app);
        }

        app.setLastUpdatedAt(Instant.now());
        Application saved = applicationRepository.save(app);

        // Publish only when status actually changed
        if (request.getStatus() != null) {
            eventPublisher.publishStatusChanged(buildStatusChangedEvent(
                    saved, previousStatus, saved.getStatus(), ApplicationSource.MANUAL));
        }

        log.info("Updated application id={} for userId={}", applicationId, userId);
        return applicationMapper.toResponse(saved);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // DELETE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public void deleteApplication(UUID userId, UUID applicationId) {
        Application app = applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        applicationRepository.delete(app);
        
        eventPublisher.publishApplicationDeleted(com.jobtracker.monolith.events.ApplicationDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("application.deleted")
                .userId(app.getUserId())
                .applicationId(app.getId())
                .timestamp(Instant.now())
                .company(app.getCompany())
                .role(app.getRole())
                .status(app.getStatus().name())
                .build());

        log.info("Deleted application id={} for userId={}", applicationId, userId);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // EMAIL STATUS DETECTED (inbound RabbitMQ event)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Processes an email.status_detected event from email-service.
     *
     * <p>Auto-detection transition rule: the <em>only</em> guard is terminal-state
     * detection. If the current status is terminal (REJECTED/WITHDRAWN), we record
     * the event as processed and return without modifying anything.
     * Offer â†’ REJECTED (rescinded offer) is therefore always honoured.
     */
    @Override
    @Transactional
    public void handleEmailStatusDetected(EmailStatusDetectedEvent event) {

        // â”€â”€ Idempotency â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (processedEventRepository.existsById(event.getEventId())) {
            log.warn("Event {} already processed â€” skipping (idempotency)", event.getEventId());
            return;
        }

        UUID              userId         = event.getUserId();
        String            company        = event.getCompany();
        String            role           = event.getRole();
        ApplicationStatus detectedStatus = null;
        try {
            detectedStatus = ApplicationStatus.valueOf(event.getDetectedStatus().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Invalid status detected: {}", event.getDetectedStatus());
            saveProcessedEvent(event.getEventId());
            return;
        }
        String            sourceEmail    = event.getSourceEmailAddress();

        Optional<Application> matchOpt = findBestMatch(userId, company, role, sourceEmail);

        if (matchOpt.isPresent()) {
            Application app = matchOpt.get();

            // â”€â”€ Guard: don't leave terminal states via auto-detection â”€â”€
            if (app.getStatus().isTerminal()) {
                log.info("Application {} is in terminal state {} â€” ignoring auto-detected status {}",
                         app.getId(), app.getStatus(), detectedStatus);
                // Still record the event so it isn't retried
                saveProcessedEvent(event.getEventId());
                return;
            }

            // â”€â”€ Same status already applied or lower status detected â”€â”€â”€â”€â”€â”€â”€â”€
            if (detectedStatus.ordinal() <= app.getStatus().ordinal()) {
                log.debug("Application {} already in status {} (detected: {}) â€” ignoring downgrade/duplicate",
                          app.getId(), app.getStatus(), detectedStatus);
                
                if (app.getSourceEmailAddress() == null && sourceEmail != null) {
                    app.setSourceEmailAddress(sourceEmail);
                    applicationRepository.save(app);
                    log.info("Backfilled sourceEmailAddress for application {}", app.getId());
                }

                saveProcessedEvent(event.getEventId());
                return;
            }

            // â”€â”€ Apply the auto-detected status â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            ApplicationStatus previousStatus = app.getStatus();
            app.setStatus(detectedStatus);
            app.setSource(ApplicationSource.AUTO);
            app.setLastUpdatedAt(Instant.now());
            if (detectedStatus == ApplicationStatus.OFFER) {
                app.setIsArchived(true);
            }
            
            if (app.getSourceEmailAddress() == null && sourceEmail != null) {
                app.setSourceEmailAddress(sourceEmail);
            }
            
            if (event.getInterviewLink() != null) app.setInterviewLink(event.getInterviewLink());
            if (event.getInterviewTime() != null) app.setInterviewTime(event.getInterviewTime());
            if (event.getAssessmentDate() != null) app.setAssessmentDate(event.getAssessmentDate());
            if (event.getNotes() != null) {
                // Append notes if they exist, else set them
                if (app.getNotes() == null || app.getNotes().isBlank()) {
                    app.setNotes(event.getNotes());
                } else {
                    app.setNotes(app.getNotes() + "\n\nEmail Snippet:\n" + event.getNotes());
                }
            }
            
            Application saved = applicationRepository.save(app);

            log.info("Auto-updated application {} from {} to {} (eventId={})",
                     saved.getId(), previousStatus, detectedStatus, event.getEventId());

            eventPublisher.publishStatusChanged(
                    buildStatusChangedEvent(saved, previousStatus, detectedStatus, ApplicationSource.AUTO));

        } else {
            // â”€â”€ No match â†’ create a new auto-detected application â”€â”€â”€
            Application app = Application.builder()
                    .userId(userId)
                    .company(company)
                    .role(role)
                    .status(detectedStatus)
                    .source(ApplicationSource.AUTO)
                    .appliedDate(LocalDate.now())
                    .lastUpdatedAt(Instant.now())
                    .isArchived(detectedStatus == ApplicationStatus.OFFER)
                    .interviewLink(event.getInterviewLink())
                    .interviewTime(event.getInterviewTime())
                    .assessmentDate(event.getAssessmentDate())
                    .notes(event.getNotes())
                    .sourceEmailAddress(sourceEmail)
                    .build();
            Application saved = applicationRepository.save(app);

            log.info("Auto-created application {} for company='{}' role='{}' status={} (eventId={})",
                     saved.getId(), company, role, detectedStatus, event.getEventId());

            // Publish both events for a newly auto-created application
            eventPublisher.publishApplicationCreated(buildCreatedEvent(saved));
            eventPublisher.publishStatusChanged(
                    buildStatusChangedEvent(saved, null, detectedStatus, ApplicationSource.AUTO));
        }

        saveProcessedEvent(event.getEventId());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Private helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Resolves the best-matching application for an email-detected event.
     *
     * <p>Match strategy (in order):
     * <ol>
     *   <li>If role is non-null â†’ case-insensitive exact match on company + role.</li>
     *   <li>If role is null (or #1 returns nothing) â†’ company substring match.<br>
     *       0 results â†’ empty.<br>
     *       1 result  â†’ use it.<br>
     *       â‰¥2 results â†’ use most-recently-updated (first element, ordered DESC by repo).</li>
     * </ol>
     */
    private Optional<Application> findBestMatch(UUID userId, String company, String role, String sourceEmail) {
        Instant sixMonthsAgo = Instant.now().minus(180, java.time.temporal.ChronoUnit.DAYS);

        if (role != null && !role.isBlank()) {
            List<Application> matches =
                    applicationRepository.findByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCase(
                            userId, company, role);
            
            matches = matches.stream()
                .filter(app -> app.getLastUpdatedAt().isAfter(sixMonthsAgo))
                .filter(app -> app.getSourceEmailAddress() == null || app.getSourceEmailAddress().equalsIgnoreCase(sourceEmail))
                .toList();

            if (!matches.isEmpty()) {
                return Optional.of(matches.get(0));
            }
        }

        // Fallback: company-only (repo method orders DESC by lastUpdatedAt)
        List<Application> companyMatches =
                applicationRepository.findByUserIdAndCompanyContainingIgnoreCaseOrderByLastUpdatedAtDesc(
                        userId, company);
        
        companyMatches = companyMatches.stream()
            .filter(app -> app.getLastUpdatedAt().isAfter(sixMonthsAgo))
            .filter(app -> app.getSourceEmailAddress() == null || app.getSourceEmailAddress().equalsIgnoreCase(sourceEmail))
            .toList();

        if (companyMatches.isEmpty()) {
            return Optional.empty();
        }

        if (companyMatches.size() > 1) {
            log.info("Multiple applications found for userId={} company='{}' (role=null) â€” " +
                     "picking most-recently-updated (id={})",
                     userId, company, companyMatches.get(0).getId());
        }

        return Optional.of(companyMatches.get(0)); // first = most recently updated
    }

    private void saveProcessedEvent(UUID eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }

    private ApplicationCreatedEvent buildCreatedEvent(Application app) {
        return ApplicationCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("application.created")
                .userId(app.getUserId())
                .timestamp(Instant.now())
                .applicationId(app.getId())
                .company(app.getCompany())
                .role(app.getRole())
                .status(app.getStatus().name())
                .source(app.getSource().name())
                .appliedDate(app.getAppliedDate())
                .build();
    }

    private StatusChangedEvent buildStatusChangedEvent(Application app,
                                                        ApplicationStatus previous,
                                                        ApplicationStatus next,
                                                        ApplicationSource source) {
        return StatusChangedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("status.changed")
                .userId(app.getUserId())
                .timestamp(Instant.now())
                .applicationId(app.getId())
                .company(app.getCompany())
                .role(app.getRole())
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(next.name())
                .source(source.name())
                .build();
    }
}
