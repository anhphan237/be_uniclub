package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.AdminSummaryResponse;
import com.example.uniclub.dto.response.ClubRankingResponse;
import com.example.uniclub.dto.response.EventRankingResponse;
import com.example.uniclub.dto.response.SystemOverviewResponse;
import com.example.uniclub.dto.response.RecommendationResponse;

import com.example.uniclub.service.AdminDashboardService;
import com.example.uniclub.service.AdminStatisticService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminStatisticService adminStatisticService;

    // ======================================================
    // 📌 1. SUMMARY
    // ======================================================
    @Operation(summary = "Tổng hợp dữ liệu hệ thống cho Admin Dashboard")
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminDashboardService.getSummary());
    }

    // ======================================================
    // 📌 2. Students by major
    // ======================================================
    @Operation(
            summary = "Thống kê số lượng sinh viên theo ngành",
            description = """
            API này trả về danh sách các ngành học cùng với số lượng sinh viên (role = STUDENT)
            đang hoạt động trong hệ thống, sắp xếp theo thứ tự giảm dần.
            """
    )
    @GetMapping("/students-by-major")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudentCountByMajor() {
        return ResponseEntity.ok(ApiResponse.ok(adminStatisticService.getStudentCountByMajor()));
    }

    // ======================================================
    // 📌 3. CLUB RANKING
    // ======================================================
    @Operation(summary = "Xếp hạng CLB hoạt động sôi nổi nhất theo tháng")
    @GetMapping("/clubs/ranking")
    public ResponseEntity<ApiResponse<List<ClubRankingResponse>>> getClubRanking(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getClubRanking(year, month))
        );
    }

    // ======================================================
    // 📌 4. EVENT POPULARITY
    // ======================================================
    @Operation(summary = "Thống kê các sự kiện được yêu thích nhất")
    @GetMapping("/events/popular")
    public ResponseEntity<ApiResponse<List<EventRankingResponse>>> getEventRanking(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getEventRanking(year, month))
        );
    }

    // ======================================================
    // 📌 5. ADVANCED OVERVIEW
    // ======================================================
    @Operation(summary = "Tổng quan nâng cao về hoạt động hệ thống")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SystemOverviewResponse>> getAdvancedOverview() {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getAdvancedOverview())
        );
    }

    // ======================================================
    // 📌 6. RECOMMENDATIONS ENGINE
    // ======================================================
    @Operation(summary = "Gợi ý đánh giá từ hệ thống cho Admin/UniStaff")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations() {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getRecommendations())
        );
    }
    @Operation(summary = "AI-powered recommendations for Admin & UniStaff")
    @GetMapping("/ai-recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getAIRecommendations() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getAIRecommendations()));
    }
}
