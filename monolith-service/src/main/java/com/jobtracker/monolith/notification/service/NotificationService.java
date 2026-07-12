package com.jobtracker.monolith.notification.service;

import com.jobtracker.monolith.notification.dto.NotificationResponse;
import com.jobtracker.monolith.notification.entity.Notification;
import com.jobtracker.monolith.notification.exception.NotificationNotFoundException;
import com.jobtracker.monolith.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Manages in-app notification read/unread state and retrieval. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listAll(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listUnread(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        n.setRead(true);
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllReadForUser(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getEventId(), n.getType(),
                n.getTitle(), n.getBody(), n.isRead(), n.getCreatedAt());
    }
}
