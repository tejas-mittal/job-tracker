package com.jobtracker.monolith.tracker.config;

import java.lang.annotation.*;

/**
 * Method parameter annotation that injects the authenticated user's UUID.
 *
 * <p>Resolution strategy (see {@link CurrentUserIdArgumentResolver}):
 * <ul>
 *   <li>Production profile â€” extracted from the {@code sub} claim of the JWT bearer token.</li>
 *   <li>Local profile â€” read from the {@code X-User-Id} request header (no JWT required).</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * @GetMapping
 * public ResponseEntity<List<ApplicationResponse>> list(@CurrentUserId UUID userId, ...) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
