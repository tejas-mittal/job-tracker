package com.jobtracker.monolith.email.dto;

import java.time.Instant;
import java.util.UUID;

/** Summarises a linked Gmail account â€” never exposes raw or encrypted tokens. */
public record GmailAccountResponse(
        UUID    id,
        String  gmailAddress,
        Instant linkedAt,
        Instant lastPolledAt,
        boolean hasSyncCursor   // true when historyId is set (incremental sync active)
) {}
