package com.example.uniclub.controller;

import com.example.uniclub.dto.response.ClubStatisticsResponse;
import com.example.uniclub.dto.response.UniversityAttendanceResponse;
import com.example.uniclub.dto.response.UniversityPointsResponse;
import com.example.uniclub.dto.response.UniversityStatisticsResponse;
import com.example.uniclub.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    // Tổng hợp toàn trường
    @GetMapping("/statistics")
    public ResponseEntity<UniversityStatisticsResponse> getUniversityStatistics() {
        return ResponseEntity.ok(universityService.getUniversitySummary());
    }

    // Tổng hợp riêng cho 1 CLB
    @GetMapping("/statistics/{clubId}")
    public ResponseEntity<ClubStatisticsResponse> getClubStatistics(@PathVariable Long clubId) {
        return ResponseEntity.ok(universityService.getClubSummary(clubId));
    }

    @GetMapping("/points")
    public ResponseEntity<UniversityPointsResponse> getPointsOverview() {
        return ResponseEntity.ok(universityService.getPointsRanking());
    }

    // ✅ API: Xếp hạng CLB theo số lượng attendance
    @GetMapping("/attendance-ranking")
    public ResponseEntity<UniversityAttendanceResponse> getAttendanceRanking() {
        return ResponseEntity.ok(universityService.getAttendanceRanking());
    }
}

