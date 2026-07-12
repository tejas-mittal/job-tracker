package com.jobtracker.monolith.tracker.service.impl;

import com.jobtracker.monolith.tracker.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationCleanupService {

    private final ApplicationRepository applicationRepository;

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @Transactional
    public void cleanupOldApplications() {
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        log.info("Starting cleanup of applications older than {}", sixMonthsAgo);
        
        int archivedCount = applicationRepository.archiveByLastUpdatedAtBefore(sixMonthsAgo);
        
        log.info("Finished cleanup. Archived {} old applications.", archivedCount);
    }
}
