package com.example.uniclub.service;

import java.time.Duration;

public interface AttendanceService {
    String generateEncryptedToken(Long eventId, Duration ttl);
    void checkIn(String token, String email);
}
