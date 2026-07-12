package com.jobtracker.monolith.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationStatusChangedEvent {
    private UUID   eventId;
    private UUID   userId;
    private UUID   applicationId;
    private String previousStatus;
    private String newStatus;
}
