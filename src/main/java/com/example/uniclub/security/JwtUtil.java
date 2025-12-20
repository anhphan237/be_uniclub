package com.example.uniclub.security;

import com.example.uniclub.entity.User;
import com.example.uniclub.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserRepository userRepo;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("app.jwt.secret is missing in application.properties");
        }

        Key tempKey;
        try {
            tempKey = Keys.hmacShaKeyFor(secret.getBytes());
        } catch (WeakKeyException e) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters long (256 bits)");
        }

        this.key = tempKey;
        this.expirationMs = expirationMs;

        logger.info("JwtUtil initialized successfully. Expiration(ms): " + expirationMs);
    }

    // ==============================================================
    //  🔥  GENERATE TOKEN CHỨA EMAIL + ROLE
    // ==============================================================
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ==============================================================
    //  ✔ BACKWARD COMPATIBLE: method cũ (1 tham số) vẫn dùng được
    // ==============================================================
//    public String generateToken(String email) {
//        return generateToken(email, "STUDENT");   // fallback
//    }

    // ==============================================================
    //  GET EMAIL (subject)
    // ==============================================================
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // ==============================================================
    //  GET ROLE từ JWT
    // ==============================================================
    public String extractRole(String token) {
        Object roleObj = getClaims(token).get("role");
        return roleObj != null ? roleObj.toString() : null;
    }

    // ==============================================================
    //  VALIDATE TOKEN
    // ==============================================================
    public boolean validateToken(String token) {
        try {
            getClaims(token); // parse được → hợp lệ
            return true;
        } catch (JwtException e) {
            logger.warning("JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    // ==============================================================
    //  INTERNAL: PARSE FULL CLAIMS
    // ==============================================================
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ==============================================================
    //  ✔ GET USER TỪ REQUEST (giữ nguyên logic cũ)
    // ==============================================================
    public User getUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);

        if (!validateToken(token)) {
            return null;
        }

        String email = extractEmail(token);

        return userRepo.findByEmail(email).orElse(null);
    }
    public String generateFullToken(
            Long userId,
            String email,
            String role,
            Long clubId,
            java.util.List<Long> clubIds,
            Boolean isStaff
    ) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("clubId", clubId)
                .claim("clubIds", clubIds)
                .claim("isStaff", isStaff)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

}
