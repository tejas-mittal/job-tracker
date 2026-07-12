package com.jobtracker.monolith.analytics.controller;

import com.jobtracker.monolith.analytics.dto.AnalyticsResponse;
import com.jobtracker.monolith.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(analyticsService.getAnalytics(userId));
    }
}
