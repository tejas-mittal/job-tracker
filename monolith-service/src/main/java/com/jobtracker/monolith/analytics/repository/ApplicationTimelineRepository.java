package com.jobtracker.monolith.analytics.repository;

import com.jobtracker.monolith.analytics.entity.ApplicationTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationTimelineRepository extends JpaRepository<ApplicationTimeline, UUID> {

    @Query("SELECT t FROM ApplicationTimeline t WHERE t.applicationId = :applicationId AND t.status = :status")
    Optional<ApplicationTimeline> findByApplicationIdAndStatus(UUID applicationId, String status);

    @Query("SELECT t FROM ApplicationTimeline t WHERE t.userId = :userId AND t.status = :status")
    List<ApplicationTimeline> findByUserIdAndStatus(UUID userId, String status);
}
