package com.jobtracker.monolith.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Classifies an email into a job application status based on subject + body keywords.
 *
 * <h3>Classification logic</h3>
 * <p>Each status has a list of compiled regex patterns. The first status whose
 * <em>any</em> pattern matches the combined subject + body text wins.
 * Priority order:
 * <ol>
 *   <li>OFFER â€” strongest positive signal</li>
 *   <li>INTERVIEW â€” scheduling / next-round signal</li>
 *   <li>REJECTED â€” rejection / decline signal</li>
 *   <li>WITHDRAWN â€” role closed / position filled signal</li>
 * </ol>
 *
 * <p>If no pattern matches, returns {@link Optional#empty()} â€” the email
 * is recorded as processed but no status event is published.
 *
 * <h3>Design rationale for regex vs. ML</h3>
 * <p>Regex is intentionally chosen: it is auditable, easily extended, requires no
 * external dependencies, and performs well for the structured language patterns
 * used in HR email templates. False positives are bounded â€” unrecognised patterns
 * simply fall through.
 */
@Slf4j
@Service
public class EmailClassifierService {

    /**
     * Detectable application statuses in priority order.
     */
    public enum DetectedStatus {
        OFFER, INTERVIEW, REJECTED, WITHDRAWN, APPLIED
    }

    private record StatusPattern(DetectedStatus status, List<Pattern> patterns) {}

    // â”€â”€ Pattern definitions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // All patterns are case-insensitive (Pattern.CASE_INSENSITIVE).
    // Each pattern is compiled once at class load time for performance.

    private static final List<StatusPattern> STATUS_PATTERNS = List.of(

        new StatusPattern(DetectedStatus.OFFER, List.of(
            compile("(?i)\\b(offer of employment|job offer|pleased to offer you (?:the|a) (?:position|role|job)|formal offer|offer letter)\\b")
        )),

        new StatusPattern(DetectedStatus.INTERVIEW, List.of(
            compile("(?i)\\b(schedule an interview|invite you to (?:an )?interview|next steps in the interview|technical interview|phone screen|interview availability|your availability|schedule a call|moving forward with your application|moving to the next round|campus interview|interview invitation|internship interview|interview scheduled|screening assessment|online assessment|coding exam|coding test|hackerrank|take the assessment|complete a screening)\\b")
        )),

        new StatusPattern(DetectedStatus.REJECTED, List.of(
            compile("(?i)\\b(unfortunately, we|regret to inform|not moving forward|not be moving forward|unable to offer|position has been filled|pursue other candidates|other applicants|move forward with another candidate|unsuccessful|not selected|decided to move forward with other)\\b")
        )),

        new StatusPattern(DetectedStatus.WITHDRAWN, List.of(
            compile("(?i)\\b(position is closed|role is closed|cancelled|withdrawn|no longer available)\\b")
        )),

        new StatusPattern(DetectedStatus.APPLIED, List.of(
            compile("(?i)\\b(thank you for applying|application received|application for (?:the |a )?(?:position|role|job|internship)|applied for|resume received|application has been received|thank you for your interest|congratulations.*applying)\\b")
        ))
    );

    /**
     * Classifies an email by matching patterns against the combined
     * subject and first 2000 characters of the body (sufficient for classification;
     * avoids processing giant email threads).
     *
     * @param subject   email subject line (may be null)
     * @param body      plain-text body (may be null)
     * @return the detected status, or empty if no pattern matched
     */
    public Optional<DetectedStatus> classify(String subject, String body) {
        String text = buildSearchText(subject, body);
        if (text.isBlank()) return Optional.empty();

        for (StatusPattern sp : STATUS_PATTERNS) {
            for (Pattern pattern : sp.patterns()) {
                if (pattern.matcher(text).find()) {
                    log.debug("Matched status={} pattern='{}'", sp.status(), pattern.pattern());
                    return Optional.of(sp.status());
                }
            }
        }

        return Optional.empty();
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static String buildSearchText(String subject, String body) {
        StringBuilder sb = new StringBuilder();
        if (subject != null) sb.append(subject).append(" ");
        if (body != null) {
            // Only examine the first 2000 chars â€” status language is always near the top
            sb.append(body, 0, Math.min(body.length(), 2000));
        }
        return sb.toString();
    }

    private static Pattern compile(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }
}
