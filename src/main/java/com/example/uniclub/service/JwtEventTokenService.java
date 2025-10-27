package com.example.uniclub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface JwtEventTokenService {
    String generateEventToken(Long eventId, int expireMinutes);
    Long parseEventId(String token);
    Jws<Claims> parseAndVerify(String token);
}

