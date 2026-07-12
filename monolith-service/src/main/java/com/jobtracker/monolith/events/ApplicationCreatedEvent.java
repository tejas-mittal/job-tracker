package com.jobtracker.monolith.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationCreatedEvent {
    private UUID eventId;
    private String eventType;
    private UUID applicationId;
    private UUID userId;
    private String company;
    private String role;
    private String status;
    private String source;
    private LocalDate appliedDate;
    
    @Builder.Default
    private Instant timestamp = Instant.now();
}
