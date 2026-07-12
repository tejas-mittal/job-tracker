package com.jobtracker.monolith.email.service;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.jobtracker.monolith.email.config.TokenEncryptionUtil;
import com.jobtracker.monolith.email.entity.GmailAccount;
import com.jobtracker.monolith.email.repository.GmailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Wraps the Google Gmail API client.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Build an authenticated {@link Gmail} client for a given {@link GmailAccount},
 *       transparently refreshing the access token when expired.</li>
 *   <li>List unread messages from the inbox.</li>
 *   <li>Fetch message subject and body.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailClientService {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String APP_NAME       = "JobTracker Email Service";

    private final TokenEncryptionUtil        encryptionUtil;
    private final GmailAccountRepository     gmailAccountRepository;

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    // â”€â”€ Public API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Builds a Gmail client for the given account, refreshing the access token
     * if it is expired or about to expire (within 60 seconds).
     */
    @Transactional
    public Gmail buildGmailClient(GmailAccount account) throws IOException {
        String accessToken = getValidAccessToken(account);

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(accessToken);

        try {
            return new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(APP_NAME)
                    .build();
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to create trusted HTTP transport", e);
        }
    }

    /**
     * Lists relevant job-related message IDs in the inbox.
     *
     * @param gmail      authenticated Gmail client
     * @param maxResults maximum number of messages to return
     * @return list of message IDs (may be empty, never null)
     */
    public List<String> listRelevantMessageIds(Gmail gmail, int maxResults) throws IOException {
        var result = gmail.users()
                .messages()
                .list("me")
                .setQ("in:inbox (interview OR application OR applying OR offer OR candidate OR position OR role OR rejected OR hiring OR job OR internship)")
                .setMaxResults((long) maxResults)
                .execute();

        List<Message> messages = result.getMessages();
        if (messages == null || messages.isEmpty()) return List.of();

        return messages.stream()
                .map(Message::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Fetches full message details for the given message ID.
     */
    public Message fetchMessage(Gmail gmail, String messageId) throws IOException {
        return gmail.users()
                .messages()
                .get("me", messageId)
                .setFormat("full")
                .execute();
    }

    /**
     * Extracts the plain-text subject from a Gmail message.
     */
    public String extractSubject(Message message) {
        if (message.getPayload() == null) return "";
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        if (headers == null) return "";
        return headers.stream()
                .filter(h -> "Subject".equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    /**
     * Extracts the plain-text body from a Gmail message.
     * Handles both single-part and multipart messages.
     */
    public String extractBody(Message message) {
        if (message.getPayload() == null) return "";
        return extractTextFromPart(message.getPayload());
    }

    // â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns a valid (non-expired) access token, refreshing it if necessary.
     */
    private String getValidAccessToken(GmailAccount account) throws IOException {
        if (!account.isAccessTokenExpired()) {
            return encryptionUtil.decrypt(account.getAccessTokenEnc());
        }

        log.info("Access token expired for account {}; refreshing...", account.getGmailAddress());
        String rawRefreshToken = encryptionUtil.decrypt(account.getRefreshTokenEnc());

        try {
            TokenResponse tokenResponse = new RefreshTokenRequest(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new GenericUrl(TOKEN_ENDPOINT),
                    rawRefreshToken)
                    .setClientAuthentication(
                            new ClientParametersAuthentication(clientId, clientSecret))
                    .execute();

            String newAccessToken = tokenResponse.getAccessToken();
            Instant newExpiry = Instant.now().plusSeconds(
                    tokenResponse.getExpiresInSeconds() != null
                            ? tokenResponse.getExpiresInSeconds()
                            : 3600L);

            // Update the stored encrypted access token
            account.setAccessTokenEnc(encryptionUtil.encrypt(newAccessToken));
            account.setTokenExpiry(newExpiry);
            gmailAccountRepository.save(account);

            log.info("Access token refreshed for account {}", account.getGmailAddress());
            return newAccessToken;

        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to create transport for token refresh", e);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to refresh access token for " + account.getGmailAddress(), e);
        }
    }

    private String extractTextFromPart(MessagePart part) {
        String mimeType = part.getMimeType();

        // Single-part text/plain
        if ("text/plain".equals(mimeType) && part.getBody() != null
                && part.getBody().getData() != null) {
            return decodeBase64Url(part.getBody().getData());
        }

        // Single-part text/html
        if ("text/html".equals(mimeType) && part.getBody() != null
                && part.getBody().getData() != null) {
            String html = decodeBase64Url(part.getBody().getData());
            
            // Strip head, style, and script completely before other tags
            html = html.replaceAll("(?i)(?s)<head.*?</head>", " ")
                       .replaceAll("(?i)(?s)<style[^>]*>.*?</style>", " ")
                       .replaceAll("(?i)(?s)<script[^>]*>.*?</script>", " ");
                       
            // Extract a hrefs and append them to the anchor text to preserve links
            html = html.replaceAll("(?i)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", "$2 ($1)");
            
            // Naive HTML strip for classification purposes
            return html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").replaceAll("&[a-zA-Z0-9#]+;", " ").trim();
        }

        // Multipart â€” recurse into parts
        if (mimeType != null && mimeType.startsWith("multipart/") && part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                String text = extractTextFromPart(child);
                if (text != null && !text.isBlank()) return text;
            }
        }

        return "";
    }

    private static String decodeBase64Url(String encoded) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
