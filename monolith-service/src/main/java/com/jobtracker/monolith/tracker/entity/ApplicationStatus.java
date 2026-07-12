package com.jobtracker.monolith.tracker.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Possible lifecycle states of a job application.
 *
 * <p>Transition rules are encoded here so that any layer (service, tests)
 * can query them without duplicating logic.
 */
public enum ApplicationStatus {

    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN;

    /**
     * Allowed transitions for <em>manual</em> status updates initiated by the user.
     * Terminal states map to an empty set â€” no further transitions allowed.
     */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> MANUAL_TRANSITIONS =
            Map.of(
                    APPLIED,   EnumSet.of(ASSESSMENT, INTERVIEW, REJECTED, WITHDRAWN),
                    ASSESSMENT, EnumSet.of(INTERVIEW, OFFER, REJECTED, WITHDRAWN),
                    INTERVIEW, EnumSet.of(OFFER, REJECTED, WITHDRAWN),
                    OFFER,     EnumSet.of(REJECTED, WITHDRAWN),   // REJECTED = rescinded offer
                    REJECTED,  EnumSet.noneOf(ApplicationStatus.class),
                    WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class)
            );

    /**
     * Returns true if a manual update from {@code this} to {@code next} is valid.
     * Manual edits allow any transition so the user can fix mistakes (e.g. un-reject).
     */
    public boolean canManuallyTransitionTo(ApplicationStatus next) {
        return true;
    }

    /**
     * Terminal states cannot be left by <em>either</em> manual or auto-detected transitions.
     * Auto-detection uses only this check (no rank-based ordering).
     */
    public boolean isTerminal() {
        return this == REJECTED || this == WITHDRAWN;
    }
}
