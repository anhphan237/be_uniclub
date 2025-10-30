package com.example.uniclub.service.impl;

import com.example.uniclub.exception.ApiException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtEventTokenServiceImpl implements com.example.uniclub.service.JwtEventTokenService {

    private static final String SECRET = "your-super-secret-key-for-event-jwt-token-123456789012345"; // ≥ 32 ký tự
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // =========================================================
    // ✅ Sinh token cho event (có cả eventId & phase)
    // =========================================================
    @Override
    public String generateEventToken(Long eventId, String phase) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(eventId + ":" + phase) // "34:START"
                .claim("eventId", eventId)
                .claim("phase", phase)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2, ChronoUnit.MINUTES))) // 2 phút TTL
                .setId(UUID.randomUUID().toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================
    // ✅ Parse token -> lấy eventId
    // =========================================================
    @Override
    public Long parseEventId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Ưu tiên đọc từ claim
            Long eventId = claims.get("eventId", Long.class);
            if (eventId != null) return eventId;

            // Nếu không có claim thì lấy từ subject "34:START"
            String sub = claims.getSubject();
            String[] parts = sub.split(":");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired event token");
        }
    }

    // =========================================================
    // ✅ Parse & verify toàn bộ token
    // =========================================================
    @Override
    public Jws<Claims> parseAndVerify(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired event token");
        }
    }
}
