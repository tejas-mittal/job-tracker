package com.jobtracker.monolith.analytics.service;

import com.jobtracker.monolith.analytics.dto.AnalyticsResponse;
import com.jobtracker.monolith.analytics.entity.AnalyticsSnapshot;
import com.jobtracker.monolith.analytics.repository.AnalyticsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UUID userId) {
        AnalyticsSnapshot snap = snapshotRepository.findById(userId)
                .orElse(new AnalyticsSnapshot(userId)); // default empty snapshot if none exists

        return new AnalyticsResponse(
                snap.getUserId(),
                snap.getTotalApplications(),
                snap.getActiveApplications(),
                snap.getRejectedApplications(),
                snap.getOffersReceived(),
                snap.getFunnelApplied(),
                snap.getFunnelInterview(),
                snap.getFunnelOffer(),
                snap.getAvgDaysToOffer(),
                snap.getUpdatedAt()
        );
    }
}
