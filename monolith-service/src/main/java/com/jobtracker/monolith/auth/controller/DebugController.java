package com.jobtracker.monolith.auth.controller;

import com.jobtracker.monolith.auth.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DebugController {

    private final JwtUtil jwtUtil;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @GetMapping("/auth/debug-token")
    public Map<String, Object> debugToken(@RequestParam("token") String token) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        try {
            var claims = jwtUtil.validateAndExtractClaims(token);
            result.put("jjwt_success", true);
            result.put("jjwt_sub", claims.getSubject());
        } catch (Exception e) {
            result.put("jjwt_error", e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(
                            jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
            var jwt = decoder.decode(token);
            result.put("nimbus_success", true);
            result.put("nimbus_sub", jwt.getSubject());
        } catch (Exception e) {
            result.put("nimbus_error", e.getClass().getName() + ": " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/auth/debug-headers")
    public Map<String, String> debugHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            map.put(key, request.getHeader(key));
        }
        return map;
    }
}
