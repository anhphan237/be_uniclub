package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.service.UniversityOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/university/overview")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('UNIVERSITY_STAFF')")
public class UniversityOverviewController {

    private final UniversityOverviewService overviewService;

    // ============================================================
    // 🔵 API 1 — Tổng quan toàn bộ thời gian
    // ============================================================
    @Operation(summary = "Thống kê tổng quan toàn bộ CLB (host + cohost)")
    @GetMapping("/clubs")
    public ResponseEntity<?> getAllClubOverview() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        overviewService.getAllClubOverview()
                )
        );
    }

    // ============================================================
    // 🔵 API 2 — Tổng quan theo tháng (year, month)
    // ============================================================
    @Operation(summary = "Thống kê tổng quan CLB theo tháng (host + cohost)")
    @GetMapping("/clubs/month")
    public ResponseEntity<?> getAllClubOverviewByMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        overviewService.getAllClubOverviewByMonth(year, month)
                )
        );
    }
}
