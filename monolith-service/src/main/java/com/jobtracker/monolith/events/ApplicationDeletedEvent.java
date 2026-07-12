package com.jobtracker.monolith.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDeletedEvent {
    private UUID eventId;
    private String eventType;
    private UUID userId;
    private UUID applicationId;
    private Instant timestamp;
    private String company;
    private String role;
    private String status;
}
