package com.jobtracker.monolith.notification.repository;

import com.jobtracker.monolith.notification.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByUserIdAndActiveTrue(UUID userId);

    List<Webhook> findByUserId(UUID userId);

    Optional<Webhook> findByIdAndUserId(UUID id, UUID userId);
}
