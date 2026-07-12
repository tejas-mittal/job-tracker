package com.jobtracker.monolith.tracker.repository;

import com.jobtracker.monolith.tracker.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for Application entities.
 *
 * <p>IMPORTANT: This repository must only be accessed from the service layer.
 * Controllers and RabbitMQ listeners must not reference it directly.
 *
 * <p>JpaSpecificationExecutor enables dynamic filter queries via
 * {@link ApplicationSpecification} â€” used by the list endpoint.
 */
public interface ApplicationRepository
        extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    /**
     * Finds a single application by its ID and owning user.
     * Returns empty if not found OR if the userId does not match â€”
     * both cases map to a 404 response (no record-existence leakage).
     */
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Email match: company + role (case-insensitive exact match on both).
     * Used when the detected email contains a non-null role.
     */
    List<Application> findByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCase(
            UUID userId, String company, String role);

    /**
     * Email match fallback: company only (case-insensitive substring match).
     * Used when role is null in the detected event.
     * Ordered by lastUpdatedAt DESC so the service can easily pick the
     * most-recently-updated record when multiple results are returned.
     */
    List<Application> findByUserIdAndCompanyContainingIgnoreCaseOrderByLastUpdatedAtDesc(
            UUID userId, String company);

    /**
     * Archives applications older than the given timestamp.
     * Returns the number of archived applications.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Application a SET a.isArchived = true WHERE a.lastUpdatedAt < :expiryDate AND a.isArchived = false")
    int archiveByLastUpdatedAtBefore(@org.springframework.data.repository.query.Param("expiryDate") java.time.Instant expiryDate);
}
