package com.example.uniclub.service;

import java.time.Duration;
import java.util.Map;

public interface AttendanceService {
    Map<String, String> getQrTokenForEvent(Long eventId);  // Leader lấy QR token (regen mỗi 5')
    void checkInWithToken(String token, String email);     // Member check-in
}
