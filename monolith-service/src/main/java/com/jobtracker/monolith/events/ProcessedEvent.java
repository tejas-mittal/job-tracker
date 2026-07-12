package com.jobtracker.monolith.events;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    private UUID eventId;
    private Instant processedAt;

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
