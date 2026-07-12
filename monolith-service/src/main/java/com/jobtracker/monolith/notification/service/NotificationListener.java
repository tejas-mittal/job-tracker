package com.jobtracker.monolith.notification.service;

import com.jobtracker.monolith.events.StatusChangedEvent;
import com.jobtracker.monolith.events.EmailStatusDetectedEvent;
import com.jobtracker.monolith.notification.entity.Notification;
import com.jobtracker.monolith.notification.entity.Webhook;
import com.jobtracker.monolith.notification.repository.NotificationRepository;
import com.jobtracker.monolith.notification.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationRepository notificationRepository;
    private final WebhookRepository      webhookRepository;
    private final WebhookDeliveryService webhookDeliveryService;

    @EventListener
    @Transactional
    public void onStatusChanged(StatusChangedEvent event) {
        log.info("Received status-changed event: applicationId={} {} â†’ {}",
                event.getApplicationId(), event.getPreviousStatus(), event.getNewStatus());

        String title = buildStatusChangedTitle(event);
        String body  = buildStatusChangedBody(event);

        Notification notification = persist(event.getEventId(), event.getUserId(),
                "STATUS_CHANGED", title, body);

        if (notification != null) {
            deliverWebhooks(notification);
        }
    }

    @EventListener
    @Transactional
    public void onEmailStatusDetected(EmailStatusDetectedEvent event) {
        log.info("Received email-status-detected event: userId={} status={} company='{}'",
                event.getUserId(), event.getDetectedStatus(), event.getCompany());

        String title = buildEmailDetectedTitle(event);
        String body  = buildEmailDetectedBody(event);

        Notification notification = persist(event.getEventId(), event.getUserId(),
                "EMAIL_DETECTED", title, body);

        if (notification != null) {
            deliverWebhooks(notification);
        }
    }

    private Notification persist(java.util.UUID eventId, java.util.UUID userId,
                                  String type, String title, String body) {
        if (notificationRepository.existsByEventId(eventId)) {
            log.debug("Duplicate event {} â€” skipping", eventId);
            return null;
        }

        try {
            return notificationRepository.save(Notification.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .read(false)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event {} on insert (race condition) â€” skipping", eventId);
            return null;
        }
    }

    private void deliverWebhooks(Notification notification) {
        List<Webhook> webhooks = webhookRepository.findByUserIdAndActiveTrue(notification.getUserId());
        if (webhooks.isEmpty()) return;

        log.debug("Dispatching to {} webhook(s) for userId={}", webhooks.size(), notification.getUserId());
        for (Webhook webhook : webhooks) {
            webhookDeliveryService.deliver(webhook, notification);
        }
    }

    private static String buildStatusChangedTitle(StatusChangedEvent e) {
        String company = e.getCompany() != null ? e.getCompany() : "Unknown";
        return String.format("Application update: %s â†’ %s", e.getPreviousStatus(), e.getNewStatus());
    }

    private static String buildStatusChangedBody(StatusChangedEvent e) {
        String role    = e.getRole() != null ? e.getRole() : "unknown role";
        String company = e.getCompany() != null ? e.getCompany() : "unknown company";
        return String.format(
                "Your application for %s at %s has moved from %s to %s.",
                role, company, e.getPreviousStatus(), e.getNewStatus());
    }

    private static String buildEmailDetectedTitle(EmailStatusDetectedEvent e) {
        return String.format("Email detected: %s from %s",
                e.getDetectedStatus(), e.getCompany() != null ? e.getCompany() : "unknown");
    }

    private static String buildEmailDetectedBody(EmailStatusDetectedEvent e) {
        String role = e.getRole() != null ? e.getRole() : "your application";
        return String.format(
                "An email about %s at %s suggests your status is: %s. Check your dashboard for details.",
                role,
                e.getCompany() != null ? e.getCompany() : "unknown company",
                e.getDetectedStatus());
    }
}

