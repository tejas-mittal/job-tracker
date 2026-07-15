package com.jobtracker.monolith.email.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.jobtracker.monolith.email.entity.GmailAccount;
import com.jobtracker.monolith.email.entity.ProcessedEmail;
import com.jobtracker.monolith.email.repository.GmailAccountRepository;
import com.jobtracker.monolith.email.repository.ProcessedEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scheduled Gmail polling service.
 *
 * <p>Runs on the cron schedule configured by {@code polling.cron} (default: every 5 minutes).
 * For each linked Gmail account across all users:
 * <ol>
 *   <li>Builds an authenticated Gmail client (refreshes token if needed).</li>
 *   <li>Lists unread inbox messages.</li>
 *   <li>For each unread message â€” idempotency check, then classify.</li>
 *   <li>If classified â†’ extract company + role from subject, publish event.</li>
 *   <li>Record the message in {@code processed_emails} regardless of whether
 *       it was classified (prevents repeated processing).</li>
 * </ol>
 *
 * <p>Errors for a single account are caught and logged â€” they do NOT abort
 * polling of other accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailPollingService {

    private final GmailAccountRepository   gmailAccountRepository;
    private final ProcessedEmailRepository processedEmailRepository;
    private final GmailClientService       gmailClientService;
    private final AiService                aiService;
    private final EmailEventPublisher      eventPublisher;

    @Value("${polling.max-results:50}")
    private int maxResults;

    /**
     * Poll all linked Gmail accounts.
     * Cron is configurable via {@code polling.cron}.
     */
    @Scheduled(cron = "${polling.cron}")
    public void pollAllAccounts() {
        List<GmailAccount> accounts = gmailAccountRepository.findAll();
        log.debug("Polling {} linked Gmail account(s)", accounts.size());

        for (GmailAccount account : accounts) {
            try {
                pollAccount(account);
            } catch (Exception e) {
                log.error("Polling failed for account={} userId={}: {}",
                        account.getGmailAddress(), account.getUserId(), e.getMessage(), e);
            }
        }
    }

    public void pollAccountsForUser(UUID userId) {
        List<GmailAccount> accounts = gmailAccountRepository.findByUserId(userId);
        log.debug("Polling {} linked Gmail account(s) for user {}", accounts.size(), userId);

        for (GmailAccount account : accounts) {
            try {
                pollAccount(account);
            } catch (Exception e) {
                log.error("Polling failed for account={} userId={}: {}",
                        account.getGmailAddress(), account.getUserId(), e.getMessage(), e);
            }
        }
    }

    // â”€â”€ Per-account polling â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public void pollAccount(GmailAccount account) throws IOException {
        Gmail gmail = gmailClientService.buildGmailClient(account);
        List<String> messageIds = gmailClientService.listRelevantMessageIds(gmail, maxResults);

        if (messageIds.isEmpty()) {
            log.debug("No relevant messages for {}", account.getGmailAddress());
            return;
        }

        log.info("Found {} relevant message(s) for {}", messageIds.size(), account.getGmailAddress());

        for (String messageId : messageIds) {
            processMessage(gmail, account, messageId);
            try {
                // Strictly respect Groq's 30 RPM Free Tier Rate Limit (2 seconds per request)
                Thread.sleep(2100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Update last_polled_at
        account.setLastPolledAt(java.time.Instant.now());
        gmailAccountRepository.save(account);
    }

    private void processMessage(Gmail gmail, GmailAccount account, String messageId) {
        // â”€â”€ Idempotency check â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        ProcessedEmail existing = processedEmailRepository.findFirstByGmailAccountAndMessageId(account, messageId).orElse(null);
        if (existing != null && existing.getDetectedStatus() != null) {
            log.debug("Skipping already-processed messageId={}", messageId);
            return;
        }

        try {
            Message message = gmailClientService.fetchMessage(gmail, messageId);
            String subject = gmailClientService.extractSubject(message);
            String body = gmailClientService.extractBody(message);

            Optional<AiService.EmailClassificationResult> resultOpt =
                    aiService.analyzeEmail(subject, body);

            String detectedStatus = null;
            if (resultOpt.isPresent()) {
                AiService.EmailClassificationResult result = resultOpt.get();
                if (result.isJobRelated()) {
                    detectedStatus = result.status() != null && !result.status().isBlank() ? result.status() : "APPLIED";
                    log.info("AiService classified messageId={} as {}", messageId, detectedStatus);

                    eventPublisher.publish(
                            UUID.randomUUID(),
                            account.getUserId(),
                            messageId,
                            result.company(),
                            result.role(),
                            detectedStatus,
                            1.0, // hardcoded confidence for now
                            result.interviewLink(),
                            result.interviewTime(),
                            result.assessmentDate(),
                            result.notes(),
                            account.getGmailAddress()
                    );
                } else {
                    detectedStatus = "IGNORED";
                    log.info("AI marked messageId={} as NOT job-related", messageId);
                }
            } else {
                log.warn("AI classification failed or unavailable for messageId={}", messageId);
                // Return early so we don't save a permanent IGNORED/null status and can retry later
                return;
            }

            // â”€â”€ Record as processed (even if no match) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (existing != null) {
                existing.setDetectedStatus(detectedStatus);
                processedEmailRepository.save(existing);
            } else {
                processedEmailRepository.save(ProcessedEmail.builder()
                        .gmailAccount(account)
                        .messageId(messageId)
                        .detectedStatus(detectedStatus)
                        .build());
            }

        } catch (IOException e) {
            log.error("Failed to process messageId={}: {}", messageId, e.getMessage());
        }
    }

    // â”€â”€ Extraction helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Best-effort company extraction from the sender's email domain.
     * If domain is generic (e.g. gmail.com), falls back to parsing subject/body.
     */
    private String extractCompany(Message message, String subject, String body) {
        String fromHeader = "Unknown";
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            fromHeader = message.getPayload().getHeaders().stream()
                    .filter(h -> "From".equalsIgnoreCase(h.getName()))
                    .map(com.google.api.services.gmail.model.MessagePartHeader::getValue)
                    .findFirst()
                    .orElse("Unknown");
        }

        String text = (subject + " " + body).replaceAll("[\\r\\n]+", " ");
        
        java.util.regex.Pattern[] patterns = {
            java.util.regex.Pattern.compile("(?i)applying with\\s+([A-Z][A-Za-z0-9\\s]{2,29})"),
            java.util.regex.Pattern.compile("(?i)Company\\s*:\\s*([A-Za-z0-9\\s&.-]+?)(?=\\s+(?:on|for|at|has)\\b|[.,!<>\\n]|$)"),
            java.util.regex.Pattern.compile("\\bat\\s+([A-Z][A-Za-z0-9\\s]{2,29})[.,!]"), // Require capital letter for company name, no (?i)
            java.util.regex.Pattern.compile("(?i)Team\\s+([A-Z][A-Za-z0-9\\s]{2,29})"),
            java.util.regex.Pattern.compile("(?i)interest in(?: the)?\\s+([A-Z][A-Za-z0-9\\s]{2,29})[.,!]")
        };

        for (java.util.regex.Pattern p : patterns) {
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String matched = m.group(1).trim();
                // Avoid matching something like "at the moment" or very long sentences
                if (matched.length() >= 2 && matched.length() < 40 && !matched.toLowerCase().matches(".*\\b(least|the|a|an|any|all|this|that|your|our|some|moment|time)\\b.*")) {
                    return matched;
                }
            }
        }

        if (!"Unknown".equals(fromHeader)) {
            int atIdx = fromHeader.lastIndexOf('@');
            if (atIdx != -1) {
                String domain = fromHeader.substring(atIdx + 1).replaceAll("[>\"\\s].*", "");
                if (!isGenericDomain(domain)) {
                    return domain;
                }
            }
        }

        // â”€â”€ Fallback for generic domains (or forwarded test emails) â”€â”€
        
        // If all fails, return the extracted name part from the sender
        if (!"Unknown".equals(fromHeader)) {
            return extractNamePart(fromHeader);
        }
        return "Unknown";
    }

    /**
     * Best-effort role extraction from subject line.
     * Looks for patterns like "Software Engineer position" or "re: SWE Application".
     */
    private String extractRole(String subject) {
        if (subject == null || subject.isBlank()) return null;

        // Remove common prefixes like "Re:", "Fwd:"
        String cleaned = subject.replaceFirst("(?i)^(re:|fwd:|fw:)\\s*", "").trim();

        // If the subject is short enough, return it cleaned; otherwise null
        // (tracker-service handles null role via the most-recently-updated fallback)
        if (cleaned.length() <= 80) return cleaned;
        return null;
    }

    private static boolean isGenericDomain(String domain) {
        return domain.matches("(?i).*(gmail|yahoo|hotmail|outlook|protonmail|icloud)\\..*");
    }

    private static String extractNamePart(String from) {
        // Try to get the display name portion: "Name <email>" â†’ "Name"
        int angleIdx = from.indexOf('<');
        if (angleIdx > 0) return from.substring(0, angleIdx).trim().replaceAll("[\"']", "");
        return from.split("@")[0];
    }
}
