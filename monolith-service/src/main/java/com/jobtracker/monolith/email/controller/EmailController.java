package com.jobtracker.monolith.email.controller;

import com.jobtracker.monolith.email.dto.GmailAccountResponse;
import com.jobtracker.monolith.email.dto.LinkGmailResponse;
import com.jobtracker.monolith.email.service.GmailAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.jobtracker.monolith.tracker.config.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Gmail account linking and management.
 *
 * <p>Endpoint summary:
 * <ul>
 *   <li>{@code GET  /email/accounts/link}    â€” get Google OAuth2 authorization URL</li>
 *   <li>{@code GET  /email/oauth2/callback}  â€” OAuth2 callback (called by Google)</li>
 *   <li>{@code GET  /email/accounts}         â€” list linked accounts for current user</li>
 *   <li>{@code DELETE /email/accounts/{id}}  â€” unlink a Gmail account</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class EmailController {

    private final GmailAccountService gmailAccountService;
    private final com.jobtracker.monolith.email.service.GmailPollingService gmailPollingService;

    /**
     * GET /api/email/accounts/link
     * Returns the Google authorization URL. The client should redirect the user there.
     */
    @GetMapping("/api/email/accounts/link")
    public ResponseEntity<LinkGmailResponse> getLinkUrl(
            @CurrentUserId UUID userId) throws IOException {
        return ResponseEntity.ok(gmailAccountService.buildAuthorizationUrl(userId));
    }

    /**
     * GET /email/oauth2/callback
     * Receives the authorization code from Google after user grants permission.
     * Exchanges the code for tokens and links the Gmail account.
     *
     * <p>The {@code state} parameter carries the userId (set in step 1).
     * In production this is validated for CSRF; for this backend-only API
     * it is sufficient for initial implementation.
     */
    @GetMapping("/email/oauth2/callback")
    public ResponseEntity<Void> oauth2Callback(
            @RequestParam String code,
            @RequestParam String state) throws IOException {

        UUID userId = UUID.fromString(state);
        try {
            GmailAccountResponse response = gmailAccountService.handleOAuth2Callback(code, userId);
        } catch (com.jobtracker.monolith.email.exception.GmailAlreadyLinkedException e) {
            // Already linked - just proceed to redirect
        }
        
        // Trigger poll immediately asynchronously
        Thread.startVirtualThread(() -> {
            try {
                gmailPollingService.pollAllAccounts();
            } catch (Exception e) {}
        });
        
        // Redirect back to the frontend dashboard
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .location(URI.create("https://job-tracker-frontend-58r7.onrender.com/dashboard")).build();
    }

    /**
     * POST /api/email/accounts/sync
     * Triggers a manual sync of all linked Gmail accounts for the user.
     */
    @PostMapping("/api/email/accounts/sync")
    public ResponseEntity<Void> syncAccounts(@CurrentUserId UUID userId) {
        Thread.startVirtualThread(() -> {
            try {
                gmailPollingService.pollAllAccounts();
            } catch (Exception e) {}
        });
        return ResponseEntity.accepted().build();
    }

    /**
     * GET /api/email/accounts
     * Lists all Gmail accounts linked by the authenticated user.
     */
    @GetMapping("/api/email/accounts")
    public ResponseEntity<List<GmailAccountResponse>> listAccounts(
            @CurrentUserId UUID userId) {
        return ResponseEntity.ok(gmailAccountService.listAccounts(userId));
    }

    /**
     * DELETE /api/email/accounts/{id}
     * Unlinks a Gmail account. Returns 204 on success.
     */
    @DeleteMapping("/api/email/accounts/{id}")
    public ResponseEntity<Void> unlinkAccount(
            @PathVariable UUID id,
            @CurrentUserId UUID userId) {
        gmailAccountService.unlinkAccount(id, userId);
        return ResponseEntity.noContent().build();
    }
}
