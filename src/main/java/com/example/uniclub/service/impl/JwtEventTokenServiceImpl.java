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

@Service
public class JwtEventTokenServiceImpl implements com.example.uniclub.service.JwtEventTokenService {

    private static final String SECRET = "your-super-secret-key-for-event-jwt-token-123456789012345"; // 🔐 key dài ít nhất 32 ký tự
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ✅ Sinh token check-in (QR)
    public String generateEventToken(Long eventId, int expireMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject("event-checkin")
                .claim("eventId", eventId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .setId(java.util.UUID.randomUUID().toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Parse token để lấy eventId
    public Long parseEventId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("eventId", Long.class);
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired event token");
        }
    }

    // ✅ Verify & parse toàn bộ token (dùng khi cần access claims chi tiết)
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
