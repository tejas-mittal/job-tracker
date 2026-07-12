package com.jobtracker.monolith.tracker.repository;

import com.jobtracker.monolith.tracker.entity.Application;
import com.jobtracker.monolith.tracker.entity.ApplicationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specifications for dynamic Application queries.
 *
 * <p>Used exclusively by {@link com.jobtracker.monolith.tracker.service.impl.ApplicationServiceImpl}
 * to build list queries with optional filters.
 */
public final class ApplicationSpecification {

    private ApplicationSpecification() { /* utility class â€” no instantiation */ }

    public static Specification<Application> hasUserId(UUID userId) {
        return (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
    }

    public static Specification<Application> hasStatus(ApplicationStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Application> companyContains(String company) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("company")), "%" + company.toLowerCase() + "%");
    }

    /**
     * Composes a single Specification from a userId and optional filters.
     * This keeps the service layer free of Criteria API boilerplate.
     */
    public static Specification<Application> forUser(UUID userId,
                                                      ApplicationStatus status,
                                                      String company,
                                                      Boolean archived) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (company != null && !company.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("company")), "%" + company.toLowerCase() + "%")
                );
            }
            
            if (archived != null) {
                java.time.Instant sixMonthsAgo = java.time.Instant.now().minus(180, java.time.temporal.ChronoUnit.DAYS);
                if (archived) {
                    predicates.add(cb.lessThan(root.get("lastUpdatedAt"), sixMonthsAgo));
                } else {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdatedAt"), sixMonthsAgo));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
