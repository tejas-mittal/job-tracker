package com.jobtracker.monolith.analytics.service;

import com.jobtracker.monolith.events.ApplicationCreatedEvent;
import com.jobtracker.monolith.events.ApplicationStatusChangedEvent;
import com.jobtracker.monolith.events.ApplicationDeletedEvent;
import com.jobtracker.monolith.analytics.entity.AnalyticsSnapshot;
import com.jobtracker.monolith.analytics.entity.ApplicationTimeline;
import com.jobtracker.monolith.events.ProcessedEvent;
import com.jobtracker.monolith.analytics.repository.AnalyticsSnapshotRepository;
import com.jobtracker.monolith.analytics.repository.ApplicationTimelineRepository;
import com.jobtracker.monolith.tracker.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsListener {

    private final ProcessedEventRepository processedEventRepository;
    private final AnalyticsSnapshotRepository snapshotRepository;
    private final ApplicationTimelineRepository timelineRepository;

    @EventListener
    @Transactional
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        if (!shouldProcess(event.getEventId())) return;

        log.info("Processing application-created for userId={} appId={}", event.getUserId(), event.getApplicationId());

        AnalyticsSnapshot snap = getOrCreateSnapshot(event.getUserId());
        snap.setTotalApplications(snap.getTotalApplications() + 1);
        snap.setActiveApplications(snap.getActiveApplications() + 1);
        snap.setFunnelApplied(snap.getFunnelApplied() + 1);
        snap.setUpdatedAt(Instant.now());
        snapshotRepository.save(snap);

        saveTimeline(event.getUserId(), event.getApplicationId(), "APPLIED", event.getTimestamp());
    }

    @EventListener
    @Transactional
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        if (!shouldProcess(event.getEventId())) return;

        log.info("Processing status-changed for userId={} appId={} {}->{}", 
            event.getUserId(), event.getApplicationId(), event.getPreviousStatus(), event.getNewStatus());

        AnalyticsSnapshot snap = getOrCreateSnapshot(event.getUserId());
        
        // Update active/rejected/offers counts based on old->new transition
        adjustCounts(snap, event.getPreviousStatus(), event.getNewStatus());
        
        // Update funnel (we don't decrement funnel, it's 'ever reached this stage')
        if ("INTERVIEW".equals(event.getNewStatus())) {
            snap.setFunnelInterview(snap.getFunnelInterview() + 1);
        } else if ("OFFER".equals(event.getNewStatus())) {
            snap.setFunnelOffer(snap.getFunnelOffer() + 1);
        }

        snap.setUpdatedAt(Instant.now());

        // Save timeline and recalculate avg days to offer
        Instant now = Instant.now();
        saveTimeline(event.getUserId(), event.getApplicationId(), event.getNewStatus(), now);
        
        if ("OFFER".equals(event.getNewStatus())) {
            recalculateAvgDaysToOffer(snap, event.getUserId());
        }

        snapshotRepository.save(snap);
    }

    @EventListener
    @Transactional
    public void onApplicationDeleted(ApplicationDeletedEvent event) {
        if (!shouldProcess(event.getEventId())) return;

        log.info("Processing application-deleted for userId={} appId={}", event.getUserId(), event.getApplicationId());

        AnalyticsSnapshot snap = getOrCreateSnapshot(event.getUserId());
        
        snap.setTotalApplications(Math.max(0, snap.getTotalApplications() - 1));
        snap.setFunnelApplied(Math.max(0, snap.getFunnelApplied() - 1));
        
        if (!"REJECTED".equals(event.getStatus()) && !"WITHDRAWN".equals(event.getStatus()) && !"OFFER".equals(event.getStatus())) {
            snap.setActiveApplications(Math.max(0, snap.getActiveApplications() - 1));
        }
        
        if ("REJECTED".equals(event.getStatus())) {
            snap.setRejectedApplications(Math.max(0, snap.getRejectedApplications() - 1));
        }
        
        if ("OFFER".equals(event.getStatus())) {
            snap.setOffersReceived(Math.max(0, snap.getOffersReceived() - 1));
        }

        // We can optionally check if this app had interview/offer timelines and decrement those funnels too
        // For simplicity, we assume we might need to decrement if the deleted app reached those states
        if (timelineRepository.findByApplicationIdAndStatus(event.getApplicationId(), "INTERVIEW").isPresent()) {
             snap.setFunnelInterview(Math.max(0, snap.getFunnelInterview() - 1));
        }
        if (timelineRepository.findByApplicationIdAndStatus(event.getApplicationId(), "OFFER").isPresent()) {
             snap.setFunnelOffer(Math.max(0, snap.getFunnelOffer() - 1));
        }

        snap.setUpdatedAt(Instant.now());
        snapshotRepository.save(snap);
        
        // delete timelines for this app
        timelineRepository.deleteAll(timelineRepository.findByUserIdAndStatus(event.getUserId(), "APPLIED").stream().filter(t -> t.getApplicationId().equals(event.getApplicationId())).toList());
        timelineRepository.deleteAll(timelineRepository.findByUserIdAndStatus(event.getUserId(), "INTERVIEW").stream().filter(t -> t.getApplicationId().equals(event.getApplicationId())).toList());
        timelineRepository.deleteAll(timelineRepository.findByUserIdAndStatus(event.getUserId(), "OFFER").stream().filter(t -> t.getApplicationId().equals(event.getApplicationId())).toList());
        timelineRepository.deleteAll(timelineRepository.findByUserIdAndStatus(event.getUserId(), "REJECTED").stream().filter(t -> t.getApplicationId().equals(event.getApplicationId())).toList());
        timelineRepository.deleteAll(timelineRepository.findByUserIdAndStatus(event.getUserId(), "WITHDRAWN").stream().filter(t -> t.getApplicationId().equals(event.getApplicationId())).toList());
        
        recalculateAvgDaysToOffer(snap, event.getUserId());
        snapshotRepository.save(snap);
    }

    private boolean shouldProcess(java.util.UUID eventId) {
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate event {} - skipping", eventId);
            return false;
        }
        try {
            processedEventRepository.save(new ProcessedEvent(eventId));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event {} on insert - skipping", eventId);
            return false;
        }
    }

    private AnalyticsSnapshot getOrCreateSnapshot(java.util.UUID userId) {
        return snapshotRepository.findByIdForUpdate(userId).orElseGet(() -> {
            try {
                return snapshotRepository.saveAndFlush(new AnalyticsSnapshot(userId));
            } catch (DataIntegrityViolationException e) {
                return snapshotRepository.findByIdForUpdate(userId).orElseThrow();
            }
        });
    }

    private void saveTimeline(java.util.UUID userId, java.util.UUID appId, String status, Instant timestamp) {
        Optional<ApplicationTimeline> existing = timelineRepository.findByApplicationIdAndStatus(appId, status);
        if (existing.isEmpty()) {
            timelineRepository.save(ApplicationTimeline.builder()
                    .userId(userId)
                    .applicationId(appId)
                    .status(status)
                    .recordedAt(timestamp != null ? timestamp : Instant.now())
                    .build());
        }
    }

    private void adjustCounts(AnalyticsSnapshot snap, String oldStatus, String newStatus) {
        boolean oldIsTerminal = "REJECTED".equals(oldStatus) || "WITHDRAWN".equals(oldStatus) || "OFFER".equals(oldStatus);
        boolean newIsTerminal = "REJECTED".equals(newStatus) || "WITHDRAWN".equals(newStatus) || "OFFER".equals(newStatus);

        if (!oldIsTerminal && newIsTerminal) {
            snap.setActiveApplications(Math.max(0, snap.getActiveApplications() - 1));
        } else if (oldIsTerminal && !newIsTerminal) {
            snap.setActiveApplications(snap.getActiveApplications() + 1);
        }

        if ("REJECTED".equals(oldStatus)) snap.setRejectedApplications(Math.max(0, snap.getRejectedApplications() - 1));
        if ("REJECTED".equals(newStatus)) snap.setRejectedApplications(snap.getRejectedApplications() + 1);

        if ("OFFER".equals(oldStatus)) snap.setOffersReceived(Math.max(0, snap.getOffersReceived() - 1));
        if ("OFFER".equals(newStatus)) snap.setOffersReceived(snap.getOffersReceived() + 1);
    }

    private void recalculateAvgDaysToOffer(AnalyticsSnapshot snap, java.util.UUID userId) {
        List<ApplicationTimeline> offers = timelineRepository.findByUserIdAndStatus(userId, "OFFER");
        if (offers.isEmpty()) return;

        double totalDays = 0;
        int count = 0;

        for (ApplicationTimeline offerTimeline : offers) {
            Optional<ApplicationTimeline> appliedTimeline = 
                timelineRepository.findByApplicationIdAndStatus(offerTimeline.getApplicationId(), "APPLIED");
            
            if (appliedTimeline.isPresent()) {
                long days = Duration.between(appliedTimeline.get().getRecordedAt(), offerTimeline.getRecordedAt()).toDays();
                totalDays += Math.max(0, days);
                count++;
            }
        }

        if (count > 0) {
            snap.setAvgDaysToOffer(totalDays / count);
        }
    }
}
