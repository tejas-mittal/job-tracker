package com.jobtracker.monolith.tracker.controller;

import com.jobtracker.monolith.tracker.config.CurrentUserId;
import com.jobtracker.monolith.tracker.dto.ApplicationFilter;
import com.jobtracker.monolith.tracker.dto.ApplicationResponse;
import com.jobtracker.monolith.tracker.dto.CreateApplicationRequest;
import com.jobtracker.monolith.tracker.dto.UpdateApplicationRequest;
import com.jobtracker.monolith.tracker.entity.ApplicationStatus;
import com.jobtracker.monolith.tracker.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for job application CRUD operations.
 *
 * <p><strong>Architecture rules enforced here:</strong>
 * <ul>
 *   <li>Only handles HTTP concerns â€” request parsing, response shaping, status codes.</li>
 *   <li>Depends on {@link ApplicationService} interface only, never on the implementation.</li>
 *   <li>No repository access, no business logic, no entity references.</li>
 *   <li>{@code userId} is injected via {@link CurrentUserId} â€” controller doesn't know
 *       whether it came from a JWT or a header.</li>
 * </ul>
 *
 * <p>All endpoints are scoped to the authenticated user â€” applications from other users
 * are never visible (enforced in the service layer via userId ownership check).
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * POST /applications
     * Creates a new job application. Initial status is always APPLIED.
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @CurrentUserId UUID userId,
            @Valid @RequestBody CreateApplicationRequest request) {

        ApplicationResponse response = applicationService.createApplication(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /applications[?status=INTERVIEW&company=Google]
     * Lists all applications for the current user with optional filters.
     */
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> list(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Boolean archived) {

        List<ApplicationResponse> applications =
                applicationService.listApplications(userId, new ApplicationFilter(status, company, archived));
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /applications/{id}
     * Returns a single application. 404 if not found or owned by a different user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(
            @CurrentUserId UUID userId,
            @PathVariable UUID id) {

        return ResponseEntity.ok(applicationService.getApplication(userId, id));
    }

    /**
     * PATCH /applications/{id}
     * Partially updates an application (status and/or notes).
     * Returns 422 if the status transition is not allowed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationResponse> update(
            @CurrentUserId UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateApplicationRequest request) {

        return ResponseEntity.ok(applicationService.updateApplication(userId, id, request));
    }

    /**
     * DELETE /applications/{id}
     * Permanently deletes an application. Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId UUID userId,
            @PathVariable UUID id) {

        applicationService.deleteApplication(userId, id);
        return ResponseEntity.noContent().build();
    }
}
