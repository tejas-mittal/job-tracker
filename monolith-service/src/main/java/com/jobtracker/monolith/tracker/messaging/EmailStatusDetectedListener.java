package com.jobtracker.monolith.tracker.messaging;

import com.jobtracker.monolith.events.EmailStatusDetectedEvent;
import com.jobtracker.monolith.tracker.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailStatusDetectedListener {

    private final ApplicationService applicationService;

    @EventListener
    public void onEmailStatusDetected(EmailStatusDetectedEvent event) {
        log.info("Received email.status_detected event={} userId={} company='{}' status={}",
                 event.getEventId(), event.getUserId(), event.getCompany(), event.getDetectedStatus());
        applicationService.handleEmailStatusDetected(event);
    }
}
