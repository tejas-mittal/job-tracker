package com.jobtracker.monolith.email.service;

import com.jobtracker.monolith.events.EmailStatusDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes {@link EmailStatusDetectedEvent} to RabbitMQ.
 *
 * <p>The event is published to the shared topic exchange with the routing key
 * {@code email.status.detected}, which tracker-service's listener is bound to.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;



    /**
     * Publishes a detected email status event.
     *
     * @param eventId       unique ID for this event (idempotency key for tracker-service)
     * @param userId        the user who owns the linked Gmail account
     * @param gmailAddress  the Gmail address that received the email
     * @param company       extracted or empty company name
     * @param role          extracted or null role name
     * @param detectedStatus the classified status (e.g. "INTERVIEW", "REJECTED")
     * @param confidence    the confidence score
     * @param interviewLink link to interview
     * @param interviewTime extracted interview time
     * @param assessmentDate extracted assessment date
     * @param notes         extracted notes
     * @param sourceEmailAddress email address that received the job email
     */
    public void publish(UUID eventId, UUID userId, String messageId,
                         String company, String role, String detectedStatus,
                         double confidence, String interviewLink, String interviewTime,
                         String assessmentDate, String notes, String sourceEmailAddress,
                         Instant emailDate) {

        EmailStatusDetectedEvent event = EmailStatusDetectedEvent.builder()
                .eventId(eventId)
                .eventType("email.status_detected")
                .userId(userId)
                .timestamp(emailDate != null ? emailDate : Instant.now())
                .sourceEmailId(messageId)
                .company(company)
                .role(role)
                .detectedStatus(detectedStatus)
                .confidence(confidence)
                .interviewLink(interviewLink)
                .interviewTime(interviewTime)
                .assessmentDate(assessmentDate)
                .notes(notes)
                .sourceEmailAddress(sourceEmailAddress)
                .build();

        applicationEventPublisher.publishEvent(event);

        log.info("Published email.status.detected: userId={} company='{}' status={} eventId={}",
                userId, company, detectedStatus, eventId);
    }
}
