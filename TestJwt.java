import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;

public class TestJwt {
    public static void main(String[] args) {
        String secret = "default-insecure-secret-change-in-prod-minimum-32-chars";
        
        // 1. Generate token using JJWT
        var signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
                
        System.out.println("Generated token: " + token);
        
        // 2. Decode using NimbusJwtDecoder
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(
                            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
                    
            Jwt decoded = decoder.decode(token);
            System.out.println("Decoded sub: " + decoded.getSubject());
            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
