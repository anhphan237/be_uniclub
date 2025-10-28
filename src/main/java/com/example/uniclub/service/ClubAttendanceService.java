package com.example.uniclub.service;

import com.example.uniclub.dto.request.BulkAttendanceRequest;
import com.example.uniclub.dto.request.ClubAttendanceSessionRequest;
import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.security.CustomUserDetails;

import java.util.Map;

public interface ClubAttendanceService {

    Map<String, Object> getTodayAttendance(Long clubId);

    Map<String, Object> getAttendanceHistory(Long clubId, String date);

    void markAttendance(Long sessionId, Long membershipId, AttendanceStatusEnum status, String note);

    void markAll(Long sessionId, AttendanceStatusEnum status);

    Map<String, Object> getMemberAttendanceHistory(Long membershipId);

    Map<String, Object> getUniversityAttendanceOverview(String date);

    Map<String, Object> markBulk(Long sessionId, BulkAttendanceRequest request, CustomUserDetails user);

    Map<String, Object> createSession(Long clubId, ClubAttendanceSessionRequest req);
}
