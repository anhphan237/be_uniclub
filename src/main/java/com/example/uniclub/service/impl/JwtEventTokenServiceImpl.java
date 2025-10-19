package com.example.uniclub.service.impl;

import com.example.uniclub.service.JwtEventTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtEventTokenServiceImpl implements JwtEventTokenService {
    private static final String SECRET = "CHANGE_ME_TO_A_32B_SECRET_0123456789ABCDEF";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateEventToken(Long eventId, int expMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("eventId", eventId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndVerify(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
