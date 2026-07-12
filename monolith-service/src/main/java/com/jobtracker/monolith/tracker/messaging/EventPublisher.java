package com.jobtracker.monolith.tracker.messaging;

import com.jobtracker.monolith.events.ApplicationCreatedEvent;
import com.jobtracker.monolith.events.ApplicationDeletedEvent;
import com.jobtracker.monolith.events.StatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link ApplicationEventPublisher} for publishing outbound events.
 *
 * <p>Routing keys match the topic exchange binding conventions:
 * <ul>
 *   <li>{@code application.created}</li>
 *   <li>{@code status.changed}</li>
 * </ul>
 *
 * <p>Called exclusively from the service layer â€” never from controllers or listeners.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;


    /** Publish an application.created event. */
    public void publishApplicationCreated(ApplicationCreatedEvent event) {
        log.debug("Publishing application.created for applicationId={}", event.getApplicationId());
        applicationEventPublisher.publishEvent(event);
    }

    /** Publish a status.changed event. */
    public void publishStatusChanged(StatusChangedEvent event) {
        log.debug("Publishing status.changed for applicationId={} ({} â†’ {})",
                  event.getApplicationId(), event.getPreviousStatus(), event.getNewStatus());
        applicationEventPublisher.publishEvent(event);
    }

    /** Publish an application.deleted event. */
    public void publishApplicationDeleted(ApplicationDeletedEvent event) {
        log.debug("Publishing application.deleted for applicationId={}", event.getApplicationId());
        applicationEventPublisher.publishEvent(event);
    }
}
