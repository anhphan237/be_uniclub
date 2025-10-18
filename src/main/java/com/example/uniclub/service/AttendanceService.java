package com.example.uniclub.service;

public interface AttendanceService {
    String generateToken(Long eventId);
    String checkIn(String token, Long studentId);
}
