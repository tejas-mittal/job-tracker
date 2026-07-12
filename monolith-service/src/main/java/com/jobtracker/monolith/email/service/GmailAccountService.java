package com.jobtracker.monolith.email.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jobtracker.monolith.email.config.TokenEncryptionUtil;
import com.jobtracker.monolith.email.dto.GmailAccountResponse;
import com.jobtracker.monolith.email.dto.LinkGmailResponse;
import com.jobtracker.monolith.email.entity.GmailAccount;
import com.jobtracker.monolith.email.exception.GmailAccountNotFoundException;
import com.jobtracker.monolith.email.exception.GmailAlreadyLinkedException;
import com.jobtracker.monolith.email.repository.GmailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of linked Gmail accounts:
 * <ul>
 *   <li>Generate Google OAuth2 authorization URL (step 1 of linking)</li>
 *   <li>Handle OAuth2 callback â€” exchange code for tokens (step 2 of linking)</li>
 *   <li>List, and unlink accounts</li>
 * </ul>
 *
 * <p>All OAuth2 tokens are encrypted before persistence via {@link TokenEncryptionUtil}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailAccountService {

    private final GmailAccountRepository gmailAccountRepository;
    private final TokenEncryptionUtil     encryptionUtil;

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.redirect-uri}")
    private String redirectUri;

    private static final List<String> GMAIL_SCOPES = List.of(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/userinfo.email"
    );

    // â”€â”€ Step 1: Generate authorization URL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Builds the Google OAuth2 authorization URL for Gmail readonly access.
     *
     * <p>The caller should redirect the user (or return this URL) so they can
     * grant permission. After approval, Google redirects to {@code /email/oauth2/callback}
     * with a {@code code} and {@code state} query parameter.
     *
     * @param userId used as the {@code state} parameter for CSRF protection
     * @return the authorization URL to redirect to
     */
    public LinkGmailResponse buildAuthorizationUrl(UUID userId) throws IOException {
        GoogleAuthorizationCodeFlow flow = buildFlow();

        String url = new GoogleAuthorizationCodeRequestUrl(
                flow.getAuthorizationServerEncodedUrl(),
                clientId,
                redirectUri,
                GMAIL_SCOPES)
                .setState(userId.toString())
                .setAccessType("offline")   // request refresh token
                .setApprovalPrompt("force") // always show consent screen to get refresh token
                .build();

        return new LinkGmailResponse(url, "Redirect the user to the authorizationUrl to link their Gmail account.");
    }

    // â”€â”€ Step 2: OAuth2 callback â€” exchange code for tokens â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Processes the OAuth2 callback from Google.
     * Exchanges the authorization code for access + refresh tokens,
     * fetches the Gmail address, encrypts both tokens, and saves the account.
     *
     * @param code   the authorization code from Google
     * @param userId the user ID from the state parameter
     */
    @Transactional
    public GmailAccountResponse handleOAuth2Callback(String code, UUID userId) throws IOException {
        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    code,
                    redirectUri)
                    .execute();
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to create trusted transport for token exchange", e);
        }

        String accessToken  = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        Instant expiry      = Instant.now().plusSeconds(
                tokenResponse.getExpiresInSeconds() != null
                        ? tokenResponse.getExpiresInSeconds() : 3600L);

        // Fetch Gmail address to use as identifier
        String gmailAddress = fetchGmailAddress(accessToken);

        if (gmailAccountRepository.existsByUserIdAndGmailAddress(userId, gmailAddress)) {
            throw new GmailAlreadyLinkedException(gmailAddress);
        }

        GmailAccount account = GmailAccount.builder()
                .userId(userId)
                .gmailAddress(gmailAddress)
                .accessTokenEnc(encryptionUtil.encrypt(accessToken))
                .refreshTokenEnc(encryptionUtil.encrypt(refreshToken))
                .tokenExpiry(expiry)
                .build();

        GmailAccount saved = gmailAccountRepository.save(account);
        log.info("Linked Gmail account {} for userId={}", gmailAddress, userId);

        return toResponse(saved);
    }

    // â”€â”€ List accounts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public List<GmailAccountResponse> listAccounts(UUID userId) {
        return gmailAccountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // â”€â”€ Unlink account â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public void unlinkAccount(UUID accountId, UUID userId) {
        GmailAccount account = gmailAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new GmailAccountNotFoundException(accountId));

        gmailAccountRepository.delete(account);
        log.info("Unlinked Gmail account {} for userId={}", account.getGmailAddress(), userId);
    }

    // â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private GoogleAuthorizationCodeFlow buildFlow() throws IOException {
        try {
            return new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    GMAIL_SCOPES)
                    .build();
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to create trusted transport", e);
        }
    }

    /**
     * Fetches the authenticated user's Gmail address using the userinfo endpoint.
     */
    private String fetchGmailAddress(String accessToken) throws IOException {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var request   = transport.createRequestFactory()
                    .buildGetRequest(new com.google.api.client.http.GenericUrl(
                            "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken));
            var response  = request.execute();
            var json      = GsonFactory.getDefaultInstance()
                    .fromString(response.parseAsString(), com.google.api.client.util.GenericData.class);

            String email = (String) json.get("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Could not fetch Gmail address from userinfo endpoint");
            }
            return email;
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to create trusted transport for userinfo", e);
        }
    }

    private GmailAccountResponse toResponse(GmailAccount a) {
        return new GmailAccountResponse(
                a.getId(),
                a.getGmailAddress(),
                a.getLinkedAt(),
                a.getLastPolledAt(),
                a.getHistoryId() != null
        );
    }
}
