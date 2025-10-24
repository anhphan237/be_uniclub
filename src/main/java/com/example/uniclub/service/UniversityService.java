package com.example.uniclub.service;

import com.example.uniclub.dto.response.*;

public interface UniversityService {
    UniversityStatisticsResponse getUniversitySummary();
    ClubStatisticsResponse getClubSummary(Long clubId);
    UniversityPointsResponse getPointsRanking();
    UniversityAttendanceResponse getAttendanceRanking();
    AttendanceSummaryResponse getAttendanceSummary(int year);
    AttendanceSummaryResponse getAttendanceSummaryByClub(int year, Long clubId);
    AttendanceSummaryResponse getAttendanceSummaryByEvent(int year, Long eventId);
}