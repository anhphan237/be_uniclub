package com.example.uniclub.service;

import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;

import java.time.Duration;
import java.util.Map;

public interface AttendanceService {
    Map<String, String> getQrTokenForEvent(Long eventId);  // Leader lấy QR token (regen mỗi 5')
    void checkInWithToken(String token, String email);
    String verifyAttendance(Long eventId, Long userId);
    void verifyAndSaveAttendance(User user, Event event, String level);

}
