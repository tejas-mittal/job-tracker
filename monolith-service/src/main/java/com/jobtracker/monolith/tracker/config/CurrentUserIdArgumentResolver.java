package com.jobtracker.monolith.tracker.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Resolves {@link CurrentUserId} annotated UUID parameters in controller methods.
 *
 * <p><strong>Resolution order:</strong>
 * <ol>
 *   <li>If a {@link Jwt} principal is present in the SecurityContext (production profile),
 *       extract the {@code sub} claim and parse it as a UUID.</li>
 *   <li>Otherwise fall back to the {@code X-User-Id} request header
 *       (local profile â€” no JWT required for testing).</li>
 *   <li>If neither is available, throw 401 Unauthorized.</li>
 * </ol>
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserIdArgumentResolver.class);
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
               && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {

        // â”€â”€ 1. JWT principal (production / default profile) â”€â”€â”€â”€â”€â”€â”€â”€
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("CurrentUserIdArgumentResolver processing authentication: {}", auth != null ? auth.getClass().getName() : "null");
        if (auth != null) {
            logger.info("Principal class: {}", auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
        }

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                logger.error("JWT sub claim is not a valid UUID: {}", sub);
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "JWT sub claim is not a valid UUID: " + sub);
            }
        }

        // â”€â”€ 2. X-User-Id header (local profile only) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request != null) {
            String headerValue = request.getHeader(USER_ID_HEADER);
            if (headerValue != null && !headerValue.isBlank()) {
                try {
                    return UUID.fromString(headerValue.trim());
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid " + USER_ID_HEADER + " header â€” must be a UUID");
                }
            }
        }

        logger.error("User identity could not be resolved! Auth was: {}", auth);
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User identity could not be resolved. " +
                "Provide a valid JWT bearer token (prod) or X-User-Id header (local profile).");
    }
}
