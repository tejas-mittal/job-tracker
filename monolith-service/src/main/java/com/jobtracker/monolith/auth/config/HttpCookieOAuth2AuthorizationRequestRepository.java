package com.jobtracker.monolith.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the OAuth2 authorization request in memory, keyed by a short UUID
 * that is persisted in a browser cookie.
 *
 * <p>Why this approach?
 * <ul>
 *   <li>The app uses {@code SessionCreationPolicy.STATELESS}, so the default
 *       {@code HttpSessionOAuth2AuthorizationRequestRepository} cannot store
 *       state across the Google redirect.</li>
 *   <li>Serialising the full {@code OAuth2AuthorizationRequest} directly into
 *       a cookie risks exceeding the 4 KB browser cookie limit.</li>
 *   <li>Storing only a short UUID key in the cookie keeps the cookie tiny,
 *       while the real request object lives in a server-side map.</li>
 * </ul>
 *
 * <p>Note: this is an in-memory store, which is perfectly fine for a
 * single-instance deployment (Render free tier).
 */
@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_key";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    /** Server-side store: cookie-key → full authorization request. */
    private final Map<String, OAuth2AuthorizationRequest> requestCache = new ConcurrentHashMap<>();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    String key = cookie.getValue();
                    OAuth2AuthorizationRequest authRequest = requestCache.get(key);
                    log.debug("loadAuthorizationRequest: key={} found={}", key, authRequest != null);
                    return authRequest;
                })
                .orElseGet(() -> {
                    log.debug("loadAuthorizationRequest: cookie '{}' not present",
                            OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
                    return null;
                });
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        if (authorizationRequest == null) {
            // Clean up
            CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                    .ifPresent(c -> requestCache.remove(c.getValue()));
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }
        String key = UUID.randomUUID().toString();
        requestCache.put(key, authorizationRequest);
        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, key, COOKIE_EXPIRE_SECONDS);
        log.debug("saveAuthorizationRequest: saved key={}", key);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> {
                    String key = cookie.getValue();
                    OAuth2AuthorizationRequest authRequest = requestCache.remove(key);
                    CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
                    log.debug("removeAuthorizationRequest: key={} found={}", key, authRequest != null);
                    return authRequest;
                })
                .orElseGet(() -> {
                    log.debug("removeAuthorizationRequest: cookie not present");
                    return null;
                });
    }
}
