package com.jobtracker.monolith.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.monolith.notification.entity.Notification;
import com.jobtracker.monolith.notification.entity.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

/**
 * Delivers notifications to registered webhook URLs.
 *
 * <p>If the webhook has a {@code secret} configured, an
 * {@code X-Signature-256: sha256=<hex>} header is added â€” identical to the
 * GitHub webhook signing scheme.
 *
 * <p>Delivery is fire-and-forget at the call site; failures are logged but not
 * re-thrown (webhook delivery is best-effort).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final ObjectMapper objectMapper;

    @Value("${webhook.timeout-ms:5000}")
    private int timeoutMs;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Delivers a notification payload to a webhook URL.
     *
     * @param webhook      the registered webhook
     * @param notification the notification to deliver
     */
    public void deliver(Webhook webhook, Notification notification) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId",   notification.getId().toString(),
                    "userId",    notification.getUserId().toString(),
                    "type",      notification.getType(),
                    "title",     notification.getTitle(),
                    "body",      notification.getBody(),
                    "createdAt", notification.getCreatedAt().toString()
            ));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "JobTracker-Webhook/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (webhook.getSecret() != null && !webhook.getSecret().isBlank()) {
                String signature = computeHmacSha256(webhook.getSecret(), payload);
                requestBuilder.header("X-Signature-256", "sha256=" + signature);
            }

            HttpResponse<String> response = HTTP_CLIENT.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Webhook delivered: url={} status={}", webhook.getUrl(), response.statusCode());
            } else {
                log.warn("Webhook delivery returned non-2xx: url={} status={}",
                        webhook.getUrl(), response.statusCode());
            }

        } catch (Exception e) {
            log.warn("Webhook delivery failed: url={} error={}", webhook.getUrl(), e.getMessage());
        }
    }

    private static String computeHmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
