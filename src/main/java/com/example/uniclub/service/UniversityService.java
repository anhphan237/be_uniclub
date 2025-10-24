package com.example.uniclub.service;

import com.example.uniclub.dto.response.ClubStatisticsResponse;
import com.example.uniclub.dto.response.UniversityAttendanceResponse;
import com.example.uniclub.dto.response.UniversityPointsResponse;
import com.example.uniclub.dto.response.UniversityStatisticsResponse;

public interface UniversityService {
    UniversityStatisticsResponse getUniversitySummary();
    ClubStatisticsResponse getClubSummary(Long clubId);
    UniversityPointsResponse getPointsRanking();
    UniversityAttendanceResponse getAttendanceRanking();
}