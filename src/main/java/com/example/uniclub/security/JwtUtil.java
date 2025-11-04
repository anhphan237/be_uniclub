package com.example.uniclub.security;

import com.example.uniclub.entity.User;
import com.example.uniclub.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

import java.security.Key;
import java.util.Date;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());

    private final Key key;
    private final long expirationMs;

    @Autowired
    private UserRepository userRepo; // ✅ Thêm để lấy user từ DB

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

    // 🔹 Sinh token JWT
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

    // 🔹 Kiểm tra token hợp lệ
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            logger.warning("⚠️ JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    // 🔹 Lấy subject (email/username) từ token
    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    // ==============================================================
    // ✅ Helper: Lấy User từ HttpServletRequest (Bearer token)
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

        String email = getSubject(token);
        if (email == null) {
            return null;
        }

        return userRepo.findByEmail(email).orElse(null);
    }
}
