package com.example.uniclub.controller;

import com.example.uniclub.dto.response.PerformanceDetailResponse;
import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.service.ActivityEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final ActivityEngineService activityService;

    // =========================================================================
    // 1️⃣ Lấy Final Score của Member trong THÁNG HIỆN TẠI
    // =========================================================================
    @Operation(
            summary = "Lấy điểm Performance cuối cùng của thành viên trong tháng hiện tại",
            description = """
                    API trả về **finalScore (0–1)** của member trong tháng đang diễn ra.<br><br>
                    • Dùng để hiển thị điểm tổng quan trên Dashboard / Profile Member.<br>
                    • Dữ liệu lấy từ bảng **MemberMonthlyActivity**.<br>
                    • Nếu tháng hiện tại chưa có dữ liệu → hệ thống **tự tính toán và lưu vào DB** rồi trả về.<br><br>
                    ⚠️ Yêu cầu member phải có membership ở trạng thái **ACTIVE** hoặc **APPROVED**.
                    """
    )
    @GetMapping("/{memberId}")
    public ResponseEntity<Double> getCurrentMonthScore(@PathVariable Long memberId) {
        return ResponseEntity.ok(activityService.calculateMemberScore(memberId));
    }


    // =========================================================================
    // 2️⃣ Lấy Chi Tiết (BaseScore / Multiplier / FinalScore)
    // =========================================================================
    @Operation(
            summary = "Lấy chi tiết Performance của thành viên trong tháng hiện tại",
            description = """
                    Trả về **chi tiết điểm hoạt động** gồm:<br>
                    • baseScore – điểm gốc từ event + session + staff + penalty<br>
                    • multiplier – hệ số nhân theo MultiplierPolicy<br>
                    • finalScore – điểm cuối cùng sau multiplier<br><br>
                    API dành cho trang **phân tích chi tiết hoạt động của member**.
                    """
    )
    @GetMapping("/{memberId}/detail")
    public ResponseEntity<PerformanceDetailResponse> getCurrentMonthScoreDetail(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(activityService.calculateMemberScoreDetail(memberId));
    }


    // =========================================================================
    // 3️⃣ Lấy Activity của Member theo tháng bất kỳ
    // =========================================================================
    @Operation(
            summary = "Lấy dữ liệu Performance của member trong một tháng chỉ định",
            description = """
                    Trả về record trong bảng **MemberMonthlyActivity** theo (year, month).<br><br>
                    Bao gồm:<br>
                    • Số event đăng ký / tham gia<br>
                    • Số buổi sinh hoạt có mặt<br>
                    • Điểm staff đánh giá<br>
                    • Tổng điểm phạt<br>
                    • BaseScore / Multiplier / FinalScore<br>
                    • ActivityLevel (Normal, Positive, Full...)<br><br>
                    ⚠️ API *không tự tính lại*, chỉ trả về dữ liệu đã được tính trước đó.
                    """
    )
    @GetMapping("/{memberId}/monthly")
    public ResponseEntity<MemberMonthlyActivity> getMonthlyActivity(
            @PathVariable Long memberId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                activityService.getMonthlyActivity(memberId, year, month)
        );
    }


    // =========================================================================
    // 4️⃣ Lấy danh sách Performance của tất cả Member trong CLB
    // =========================================================================
    @Operation(
            summary = "Lấy danh sách Performance của tất cả thành viên CLB theo tháng",
            description = """
                    Trả về danh sách **MemberMonthlyActivity** của các member ACTIVE/APPROVED trong CLB.<br><br>
                    Dùng cho Dashboard CLB:<br>
                    • Theo dõi mức độ hoạt động từng thành viên<br>
                    • Làm báo cáo tháng<br>
                    • Advisor/Leader quản lý chất lượng hoạt động<br><br>
                    ⚠️ Nếu tháng chưa có dữ liệu → cần chạy ActivityEngine trước.
                    """
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','CLUB_VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<MemberMonthlyActivity>> getClubMonthlyActivities(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                activityService.getClubMonthlyActivities(clubId, year, month)
        );
    }


    // =========================================================================
    // 5️⃣ Ranking thành viên theo điểm Performance
    // =========================================================================
    @Operation(
            summary = "Xếp hạng thành viên CLB theo điểm Performance",
            description = """
                    Trả về danh sách thành viên sắp xếp theo **finalScore giảm dần**.<br><br>
                    Dùng cho:<br>
                    • Leaderboard tháng<br>
                    • Chọn Member of the Month<br>
                    • Xét thưởng - đánh giá thi đua<br><br>
                    ⚠️ Chỉ xếp hạng các membership ACTIVE/APPROVED.
                    """
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','CLUB_VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/club/{clubId}/ranking")
    public ResponseEntity<List<MemberMonthlyActivity>> getClubRanking(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                activityService.getClubRanking(clubId, year, month)
        );
    }
}
