package com.example.uniclub.controller;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.service.ClubMonthlyActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/club-activity")
@RequiredArgsConstructor
@Tag(
        name = "Club Activity Report",
        description = "API thống kê & tính toán điểm hoạt động hằng tháng của Câu Lạc Bộ"
)
public class ClubMonthlyActivityController {

    private final ClubMonthlyActivityService service;

    // ================================================================
    // 🔥 1) Tính lại điểm 1 CLB
    // ================================================================
    @PostMapping("/{clubId}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "[ADMIN] Tính lại điểm hoạt động CLB",
            description = """
                Tính toán lại toàn bộ chỉ số hoạt động của một CLB trong tháng:
                - tổng số event
                - feedback trung bình
                - tỷ lệ check-in
                - điểm hoạt động của thành viên
                - điểm staff
                - finalScore
                """
    )
    public ClubMonthlyActivityResponse recalc(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.recalculateForClub(clubId, year, month);
    }

    // ================================================================
    // 🔥 2) Lấy thông tin điểm CLB
    // ================================================================
    @GetMapping("/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Xem điểm hoạt động của 1 CLB",
            description = """
                Lấy chi tiết ClubMonthlyActivity của 1 CLB trong tháng.
                Bao gồm: event, checkin, feedback, member score, staff score, finalScore.
                """
    )
    public ClubMonthlyActivityResponse get(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getClubMonthlyActivity(clubId, year, month);
    }

    // ================================================================
    // 🔥 3) Xếp hạng CLB theo tháng
    // ================================================================
    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Xếp hạng các CLB theo finalScore",
            description = "Trả về danh sách CLB sắp xếp theo điểm giảm dần."
    )
    public List<ClubMonthlyActivityResponse> ranking(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getClubRanking(year, month);
    }

    // ================================================================
    // 🔥 4) Tính lại toàn bộ CLB
    // ================================================================
    @PostMapping("/recalculate-all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "[ADMIN] Tính lại điểm toàn bộ CLB",
            description = "Tính lại điểm hoạt động của tất cả CLB trong trường cho 1 tháng."
    )
    public List<ClubMonthlyActivityResponse> recalcAll(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.recalculateAllClubs(year, month);
    }

    // ================================================================
    // 🔥 5) Kiểm tra record tồn tại
    // ================================================================
    @GetMapping("/{clubId}/exists")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Kiểm tra CLB đã có record tháng hay chưa",
            description = "Trả về true nếu đã có ClubMonthlyActivity."
    )
    public boolean exists(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.exists(clubId, year, month);
    }

    // ================================================================
    // 🔥 6) Xóa record tháng (để recalc mới)
    // ================================================================
    @DeleteMapping("/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "[ADMIN] Xóa record tháng của CLB",
            description = "Dùng khi cần xoá record để tính lại từ đầu."
    )
    public void deleteMonthRecord(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        service.deleteMonthlyRecord(clubId, year, month);
    }

    // ================================================================
    // 🔥 7) Trending – CLB tăng trưởng mạnh nhất
    // ================================================================
    @GetMapping("/trending")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "CLB tăng trưởng mạnh nhất tháng",
            description = """
                So sánh finalScore tháng này và tháng trước.
                Trả về danh sách CLB tăng mạnh nhất theo % hoặc số điểm tăng thêm.
                """
    )
    public List<ClubTrendingResponse> trending(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getTrendingClubs(year, month);
    }

    // ================================================================
    // 🔥 8) History – biểu đồ 12 tháng của CLB
    // ================================================================
    @GetMapping("/{clubId}/history")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Lịch sử điểm CLB (12 tháng gần nhất)",
            description = "Dùng cho biểu đồ line chart trong dashboard."
    )
    public List<ClubMonthlyHistoryPoint> history(
            @PathVariable Long clubId,
            @RequestParam int year
    ) {
        return service.getClubHistory(clubId, year);
    }

    // ================================================================
    // 🔥 9) Breakdown – phân tích vì sao điểm cao/thấp
    // ================================================================
    @GetMapping("/{clubId}/breakdown")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Phân tích chi tiết điểm hoạt động của CLB",
            description = "Giúp leader hiểu CLB mạnh/yếu ở điểm nào."
    )
    public ClubMonthlyBreakdownResponse breakdown(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getBreakdown(clubId, year, month);
    }

    // ================================================================
    // 🔥 10) Compare – so sánh 2 CLB
    // ================================================================
    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "So sánh hai CLB",
            description = "Trả về bảng so sánh các chỉ số giữa 2 CLB."
    )
    public ClubCompareResponse compare(
            @RequestParam Long clubA,
            @RequestParam Long clubB,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.compareClubs(clubA, clubB, year, month);
    }

    // ================================================================
    // 🔥 11) Event contribution – event đóng góp bao nhiêu % điểm CLB
    // ================================================================
    @GetMapping("/{clubId}/events")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Đóng góp của từng event vào điểm CLB",
            description = "Phân tích mức ảnh hưởng từng sự kiện."
    )
    public List<ClubEventContributionResponse> eventImpact(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getEventContribution(clubId, year, month);
    }


    @PostMapping("/{clubId}/lock")
    @Operation(
            summary = "Khoá dữ liệu hoạt động tháng của CLB (chỉ ADMIN hoặc UNI STAFF)",
            description = """
        Khoá lại toàn bộ dữ liệu hoạt động tháng của CLB sau khi đã kiểm tra và xác nhận.
        Khi bị khoá, dữ liệu:
        - Không thể tính lại (không được phép recalculate)
        - Không thể chỉnh sửa
        - Sẵn sàng cho bước duyệt điểm thưởng

        Chỉ ADMIN hoặc UNIVERSITY_STAFF mới có quyền thao tác.
        """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ClubMonthlyActivityResponse lockMonth(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.lockMonthlyRecord(clubId, year, month);
    }


    @PostMapping("/{clubId}/approve")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    @Operation(
            summary = "Duyệt cấp điểm thưởng tháng cho CLB (uni staff / admin)",
            description = """
        Thao tác này sẽ:
        1. Lấy điểm thưởng (rewardPoints) được tính toán của CLB trong tháng.
        2. Cộng số điểm đó vào ví CLB (club wallet).
        3. Khoá bản ghi tháng để đảm bảo không thể thay đổi sau khi đã duyệt.
        
        Sử dụng trong bước cuối của quy trình chấm hoạt động CLB:
        - Chỉ UNIVERSITY_STAFF hoặc ADMIN mới được phép duyệt.
        - Sau khi duyệt, CLB sẽ nhận điểm vào ví để phân bổ cho thành viên.
        """
    )
    public ClubRewardApprovalResponse approveReward(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.approveRewardPoints(clubId, year, month);
    }
    @GetMapping("/monthly-summary")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Tổng quan hoạt động các CLB trong tháng",
            description = """
        Trả về danh sách tất cả CLB cùng thống kê hoạt động trong tháng:
        - Số lượng sự kiện đã tổ chức
        - Số sự kiện hoàn thành
        - Tỉ lệ thành công
        - Tổng lượt check-in
        - Điểm feedback trung bình

        Dùng cho UniStaff theo dõi tình hình hoạt động CLB theo tháng.
        """
    )
    public List<ClubMonthlySummaryResponse> monthlySummary(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return service.getMonthlySummary(year, month);
    }


}
