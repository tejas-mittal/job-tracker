package com.jobtracker.monolith.analytics.repository;

import com.jobtracker.monolith.analytics.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AnalyticsSnapshot s WHERE s.userId = :userId")
    Optional<AnalyticsSnapshot> findByIdForUpdate(@Param("userId") UUID userId);
}
