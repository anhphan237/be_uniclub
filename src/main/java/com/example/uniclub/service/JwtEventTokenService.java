package com.example.uniclub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface JwtEventTokenService {
    String generateEventToken(Long eventId, int expMinutes);
    Jws<Claims> parseAndVerify(String token);
}
