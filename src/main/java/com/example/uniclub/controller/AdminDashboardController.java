package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.*;

import com.example.uniclub.service.AdminDashboardService;
import com.example.uniclub.service.AdminStatisticService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin & UniStaff Dashboard",
        description = "API thống kê – phân tích dành cho Admin và University Staff")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminStatisticService adminStatisticService;

    // ======================================================
    // 📌 1. TỔNG QUAN HỆ THỐNG
    // ======================================================
    @Operation(
            summary = "Tổng hợp thống kê hệ thống",
            description = """
                    API này trả về số liệu tổng quan toàn hệ thống:
                    • Tổng số người dùng
                    • Tổng số CLB
                    • Tổng số sự kiện
                    • Tổng số lượt redeem
                    • Tổng số giao dịch ví điểm
                    
                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminDashboardService.getSummary());
    }

    // ======================================================
    // 📌 2. THỐNG KÊ SINH VIÊN THEO NGÀNH
    // ======================================================
    @Operation(
            summary = "Thống kê số lượng sinh viên theo ngành",
            description = """
                    Trả về số lượng sinh viên theo từng ngành học:
                    • Mã ngành (VD: SE, AI, BA)
                    • Số lượng sinh viên
                    
                    Kết quả được sắp xếp giảm dần theo số lượng.
                    
                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
    @GetMapping("/students-by-major")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudentCountByMajor() {
        return ResponseEntity.ok(ApiResponse.ok(adminStatisticService.getStudentCountByMajor()));
    }

    // ======================================================
    // 📌 3. XẾP HẠNG CLB
    // ======================================================
    @Operation(
            summary = "Xếp hạng CLB hoạt động sôi nổi nhất theo tháng",
            description = """
                    Trả về bảng xếp hạng CLB theo tháng dựa trên nhiều tiêu chí:
                    • Điểm final trung bình của thành viên
                    • Số sự kiện hoàn thành
                    • Số buổi sinh hoạt CLB
                    • Tỉ lệ check-in của sự kiện
                    • HeatScore (0–100)

                    Tham số:
                    • year – Năm (bắt buộc)
                    • month – Tháng (bắt buộc)

                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
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
    // 📌 4. MỨC ĐỘ PHỔ BIẾN CỦA SỰ KIỆN
    // ======================================================
    @Operation(
            summary = "Thống kê sự kiện được yêu thích nhất",
            description = """
                    Trả về dữ liệu thống kê mức độ thu hút của sự kiện:
                    • Số lượt đăng ký
                    • Số lượt check-in
                    • Tỉ lệ check-in
                    • Số lượng staff hỗ trợ
                    • Popularity Score (0–100)

                    Tham số:
                    • year – năm (tùy chọn)
                    • month – tháng (tùy chọn)
                    Nếu không truyền tham số → trả về toàn bộ sự kiện đã hoàn thành.

                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
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
    // 📌 5. TỔNG QUAN NÂNG CAO
    // ======================================================
    @Operation(
            summary = "Báo cáo tổng quan nâng cao",
            description = """
                    Báo cáo tổng hợp cấp độ hệ thống:
                    • Tổng số CLB
                    • Tổng số sự kiện
                    • Số sự kiện hoàn thành
                    • Số lượng thành viên đang hoạt động
                    • Tổng số giao dịch
                    • Điểm trung bình CLB
                    • Tỉ lệ check-in trung bình sự kiện
                    
                    Dùng cho dashboard tổng hợp cấp trường.
                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SystemOverviewResponse>> getAdvancedOverview() {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getAdvancedOverview())
        );
    }

    // ======================================================
    // 📌 6. RECOMMENDATIONS (RULE-BASED)
    // ======================================================
    @Operation(
            summary = "Gợi ý từ hệ thống (Rule-based)",
            description = """
                    Hệ thống phân tích dữ liệu và đưa ra gợi ý dựa trên các quy tắc cố định:
                    • CLB không hoạt động
                    • CLB không tổ chức buổi sinh hoạt
                    • Sự kiện có tỉ lệ check-in thấp
                    • Hoạt động toàn trường thấp
                    
                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations() {
        return ResponseEntity.ok(
                ApiResponse.ok(adminDashboardService.getRecommendations())
        );
    }

    // ======================================================
    // 📌 7. AI RECOMMENDATIONS (NÂNG CAO)
    // ======================================================
    @Operation(
            summary = "Gợi ý nâng cao sử dụng thuật toán phân tích",
            description = """
                    Phiên bản gợi ý nâng cao có phân tích theo thời gian và xu hướng:
                    • So sánh hoạt động CLB trong 3 tháng
                    • Phát hiện CLB giảm sút / tăng trưởng mạnh
                    • Phát hiện sự kiện có anomaly (đăng ký cao nhưng check-in thấp)
                    • Kiểm tra sức khỏe hoạt động toàn trường
                    • Chỉ số HeatScore AI 2.0

                    Dùng cho phân tích chuyên sâu
                    Đối tượng sử dụng: ADMIN, UNIVERSITY STAFF
                    """
    )
    @GetMapping("/ai-recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getAIRecommendations() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getAIRecommendations()));
    }
}
