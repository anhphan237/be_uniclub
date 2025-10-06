package com.example.uniclub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());

    private final Key key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("❌ app.jwt.secret is missing in application.properties");
        }

        Key tempKey;
        try {
            tempKey = Keys.hmacShaKeyFor(secret.getBytes());
        } catch (WeakKeyException e) {
            throw new IllegalArgumentException("❌ JWT secret key must be at least 32 characters long (256 bits)");
        }

        this.key = tempKey;
        this.expirationMs = expirationMs;

        logger.info("✅ JwtUtil initialized successfully. Expiration(ms): " + expirationMs);
    }

    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warning("⚠️ Token expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warning("⚠️ Invalid JWT format: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warning("⚠️ Unsupported JWT: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warning("⚠️ Empty or null JWT: " + e.getMessage());
        } catch (JwtException e) {
            logger.warning("⚠️ JWT validation failed: " + e.getMessage());
        }
        return false;
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
