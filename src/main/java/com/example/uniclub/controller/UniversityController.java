package com.example.uniclub.controller;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/attendance-summary")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummary(year));
    }

    @GetMapping("/attendance-summary/club/{clubId}")
    public ResponseEntity<AttendanceSummaryResponse> getClubAttendanceSummary(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByClub(year, clubId));
    }

    @GetMapping("/attendance-summary/event/{eventId}")
    public ResponseEntity<AttendanceSummaryResponse> getEventAttendanceSummary(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByEvent(year, eventId));
    }

}

