package com.jobtracker.monolith.notification.controller;

import com.jobtracker.monolith.notification.dto.NotificationResponse;
import com.jobtracker.monolith.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for in-app notifications.
 *
 * <ul>
 *   <li>GET  /notifications          â€” all notifications for user (newest first)</li>
 *   <li>GET  /notifications/unread   â€” unread only</li>
 *   <li>PATCH /notifications/{id}/read   â€” mark one as read</li>
 *   <li>PATCH /notifications/read-all   â€” mark all as read, returns count</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> listAll(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.listAll(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> listUnread(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.listUnread(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(notificationService.markRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
