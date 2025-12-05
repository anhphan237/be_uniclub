package com.example.uniclub.dto.response;

import com.example.uniclub.enums.AttendanceLevelEnum;

import java.time.LocalDateTime;

public record EventAttendeeResponse(
        Long userId,
        String fullName,
        String email,
        AttendanceLevelEnum attendanceLevel,
        LocalDateTime checkinAt,
        LocalDateTime checkMidAt,
        LocalDateTime checkoutAt
) {}
