package com.example.demo.security;

import com.example.demo.entity.Citizen;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    // ✅ Loaded from environment variable — never hardcoded
    @Value("${jwt.secret}")
    private String secret;

    // ✅ Token expiry — 1 day in ms
    private static final long EXPIRATION_MS = 86_400_000L;

    // ------------------ SIGNING KEY ------------------
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ------------------ GENERATE TOKEN (email + role) ------------------
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ------------------ OVERLOAD: GENERATE FROM CITIZEN ------------------
    public String generateToken(Citizen citizen) {
        return generateToken(citizen.getEmail(), citizen.getRole().name());
    }

    // ------------------ EXTRACT EMAIL ------------------
    public String extractEmail(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null; // ✅ null-safe — callers already check for null
        }
    }

    // ------------------ EXTRACT ROLE ------------------
    public String extractRole(String token) {
        try {
            return getClaims(token).get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    // ------------------ VALIDATE TOKEN ------------------
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT empty or null: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    // ------------------ PRIVATE HELPER ------------------
    // ✅ Single place to parse claims — avoids repeating parser boilerplate
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}