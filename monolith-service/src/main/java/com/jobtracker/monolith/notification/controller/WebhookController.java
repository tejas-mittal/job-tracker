package com.jobtracker.monolith.notification.controller;

import com.jobtracker.monolith.notification.dto.WebhookRequest;
import com.jobtracker.monolith.notification.dto.WebhookResponse;
import com.jobtracker.monolith.notification.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.jobtracker.monolith.tracker.config.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for webhook registration.
 *
 * <ul>
 *   <li>POST   /webhooks          — register a webhook</li>
 *   <li>GET    /webhooks          — list registered webhooks</li>
 *   <li>DELETE /webhooks/{id}     — remove a webhook</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<WebhookResponse> register(
            @Valid @RequestBody WebhookRequest request,
            @CurrentUserId UUID userId) {
        WebhookResponse response = webhookService.register(userId, request);
        return ResponseEntity.created(URI.create("/webhooks/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WebhookResponse>> list(@CurrentUserId UUID userId) {
        return ResponseEntity.ok(webhookService.list(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @CurrentUserId UUID userId) {
        webhookService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
