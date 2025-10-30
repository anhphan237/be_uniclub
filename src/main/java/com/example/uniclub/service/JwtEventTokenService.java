package com.example.uniclub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface JwtEventTokenService {

    // ✅ Sinh token có cả eventId & phase
    String generateEventToken(Long eventId, String phase);

    // ✅ Parse token -> lấy eventId
    Long parseEventId(String token);

    // ✅ Parse đầy đủ claims khi cần
    Jws<Claims> parseAndVerify(String token);
}
