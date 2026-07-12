package com.jobtracker.monolith.tracker.repository;

import com.jobtracker.monolith.events.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Idempotency store for processed RabbitMQ events.
 *
 * <p>Before processing any inbound event, the service calls
 * {@code existsById(event.getEventId())}. If true, the event is a replay
 * and is skipped immediately without any side effects.
 *
 * <p>IMPORTANT: Only accessed from the service layer, never from listeners directly.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    // existsById(UUID) is inherited from JpaRepository â€” no extra methods needed.
}
