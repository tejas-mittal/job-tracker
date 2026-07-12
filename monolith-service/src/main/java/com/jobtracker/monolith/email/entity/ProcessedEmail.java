package com.jobtracker.monolith.email.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a Gmail message that has been successfully classified and published.
 * Acts as an idempotency guard â€” ensures each message is processed at most once.
 */
@Entity
@Table(name = "processed_emails")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gmail_account_id", nullable = false)
    private GmailAccount gmailAccount;

    /** The Gmail message ID â€” globally unique within a Gmail inbox. */
    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    /** The status we detected from this email (may be null if no match). */
    @Column(name = "detected_status", length = 50)
    private String detectedStatus;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (processedAt == null) processedAt = Instant.now();
    }
}
