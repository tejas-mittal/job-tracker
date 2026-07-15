package com.jobtracker.monolith.tracker.mapper;

import com.jobtracker.monolith.tracker.dto.ApplicationResponse;
import com.jobtracker.monolith.tracker.dto.CreateApplicationRequest;
import com.jobtracker.monolith.tracker.entity.Application;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between {@link Application} entity and its DTOs.
 *
 * <p>MapStruct was the original choice but its annotation processor is not
 * compatible with Java 26's internal compiler API changes (TypeTag removal).
 * This hand-written @Component is functionally identical and is a Spring bean
 * that can be @Autowired or mocked in tests exactly like the generated version.
 *
 * <p>Entity â†” DTO mapping conventions:
 * <ul>
 *   <li>{@code toEntity} maps only user-supplied fields; the service layer
 *       sets id, userId, status, source, lastUpdatedAt.</li>
 *   <li>{@code toResponse} maps every field â€” entity is always fully populated
 *       before this is called.</li>
 * </ul>
 */
@Component
public class ApplicationMapper {

    /**
     * Maps create-request DTO to a partial Application entity.
     * Service-owned fields (id, userId, status, source, lastUpdatedAt) are NOT set here.
     */
    public Application toEntity(CreateApplicationRequest request) {
        Application app = new Application();
        app.setCompany(request.getCompany());
        app.setRole(request.getRole());
        app.setAppliedDate(request.getAppliedDate());
        app.setNotes(request.getNotes());
        app.setInterviewTime(request.getInterviewTime());
        app.setAssessmentDate(request.getAssessmentDate());
        app.setInterviewLink(request.getInterviewLink());
        app.setIsArchived(request.isArchived());
        return app;
    }

    /**
     * Maps a fully-populated Application entity to its immutable response record.
     */
    public ApplicationResponse toResponse(Application app) {
        return new ApplicationResponse(
                app.getId(),
                app.getUserId(),
                app.getCompany(),
                app.getRole(),
                app.getStatus(),
                app.getSource(),
                app.getAppliedDate(),
                app.getLastUpdatedAt(),
                app.getNotes(),
                app.getInterviewLink(),
                app.getInterviewTime(),
                app.getAssessmentDate(),
                app.getSourceEmailAddress(),
                app.getSourceMessageId(),
                app.getIsArchived()
        );
    }
}
