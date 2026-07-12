package com.jobtracker.monolith.notification.service;

import com.jobtracker.monolith.notification.dto.WebhookRequest;
import com.jobtracker.monolith.notification.dto.WebhookResponse;
import com.jobtracker.monolith.notification.entity.Webhook;
import com.jobtracker.monolith.notification.exception.WebhookNotFoundException;
import com.jobtracker.monolith.notification.repository.WebhookRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;

    @Transactional
    public WebhookResponse register(UUID userId, @Valid WebhookRequest request) {
        Webhook w = Webhook.builder()
                .userId(userId)
                .url(request.url())
                .secret(request.secret())
                .active(true)
                .build();
        return toResponse(webhookRepository.save(w));
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> list(UUID userId) {
        return webhookRepository.findByUserId(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID webhookId, UUID userId) {
        Webhook w = webhookRepository.findByIdAndUserId(webhookId, userId)
                .orElseThrow(() -> new WebhookNotFoundException(webhookId));
        webhookRepository.delete(w);
        log.info("Deleted webhook {} for userId={}", webhookId, userId);
    }

    private WebhookResponse toResponse(Webhook w) {
        return new WebhookResponse(w.getId(), w.getUrl(), w.isActive(), w.getCreatedAt());
    }
}
