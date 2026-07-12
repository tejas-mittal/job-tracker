package com.jobtracker.monolith.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.monolith.auth.dto.TokenResponse;
import com.jobtracker.monolith.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles a successful Google OAuth2 login.
 *
 * <p>Spring Security calls this after it has:
 * <ol>
 *   <li>Exchanged the authorization code for tokens with Google.</li>
 *   <li>Fetched the user's profile from Google's userinfo endpoint.</li>
 *   <li>Resolved the principal as an {@link OAuth2User}.</li>
 * </ol>
 *
 * <p>This handler:
 * <ol>
 *   <li>Extracts {@code sub}, {@code email}, {@code name}, {@code picture}
 *       from the Google principal.</li>
 *   <li>Delegates to {@link AuthService#loginOrRegister} (upserts user, issues tokens).</li>
 *   <li>Writes the {@link TokenResponse} as JSON to the HTTP response body.</li>
 * </ol>
 *
 * <p>Gated by {@code @Profile("!local")} â€” the local profile has no real OAuth2 flow.
 */
@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String googleId   = principal.getAttribute("sub");
        String email      = principal.getAttribute("email");
        String name       = principal.getAttribute("name");
        String pictureUrl = principal.getAttribute("picture");

        log.info("Google OAuth2 login success: googleId={} email={}", googleId, email);

        TokenResponse tokens = authService.loginOrRegister(googleId, email, name, pictureUrl);

        // Redirect back to the frontend with the JWT
        String frontendRedirectUrl = frontendUrl + "/oauth2/callback?token=" + tokens.accessToken();
        log.info("Redirecting to frontend: {}", frontendRedirectUrl);
        response.sendRedirect(frontendRedirectUrl);
    }
}
