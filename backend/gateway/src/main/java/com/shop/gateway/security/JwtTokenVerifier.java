package com.shop.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenVerifier {

    private final SecretKey secretKey;

    public JwtTokenVerifier(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtClaims verify(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Missing subject");
        }
        Object rawRoles = claims.get("roles");
        List<String> roles = rawRoles instanceof Collection<?> col
            ? col.stream().map(Object::toString).toList()
            : List.of();
        return new JwtClaims(subject, roles);
    }

    public record JwtClaims(String userId, List<String> roles) {
    }
}
