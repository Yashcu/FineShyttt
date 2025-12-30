package com.yash.fineshyttt.security;

import com.yash.fineshyttt.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-ttl-seconds}")
    private long accessTokenTtl;

    private Key signingKey;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // =========================
    // TOKEN CREATION
    // =========================
    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("roles",
                        user.getRoles().stream()
                                .map(r -> r.getName())
                                .collect(Collectors.toSet()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTokenTtl)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================
    // TOKEN VALIDATION
    // =========================
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // =========================
    // INTERNAL
    // =========================
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
