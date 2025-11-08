package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.BulkAttendanceRequest;
import com.example.uniclub.dto.request.ClubAttendanceSessionRequest;
import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(
        name = "Club Attendance Management",
        description = """
        Quản lý điểm danh câu lạc bộ (CLB) bao gồm:
        - Tạo buổi điểm danh (session)
        - Điểm danh từng thành viên hoặc hàng loạt
        - Xem lịch sử điểm danh theo CLB, theo thành viên hoặc toàn trường
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/club-attendance")
@RequiredArgsConstructor
public class ClubAttendanceController {

    private final ClubAttendanceService attendanceService;
    private final MembershipRepository membershipRepo;

    // ==========================================================
    // 📅 1. LẤY DANH SÁCH ĐIỂM DANH HÔM NAY (TỰ TẠO SESSION NẾU CHƯA CÓ)
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách điểm danh hôm nay",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Nếu chưa có buổi điểm danh cho ngày hôm nay → hệ thống tự động tạo mới.<br>
                Trả về danh sách thành viên và trạng thái điểm danh.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy dữ liệu thành công")
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @GetMapping("/{clubId}/today")
    public ApiResponse<Map<String, Object>> getTodayAttendance(@PathVariable Long clubId) {
        return ApiResponse.ok(attendanceService.getTodayAttendance(clubId));
    }

    // ==========================================================
    // 📜 2. XEM LỊCH SỬ ĐIỂM DANH CLB THEO NGÀY
    // ==========================================================
    @Operation(
            summary = "Xem lịch sử điểm danh CLB theo ngày",
            description = """
                Dành cho **CLUB_LEADER** hoặc **UNIVERSITY_STAFF**.<br>
                Truyền ngày để xem danh sách điểm danh đã lưu.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy lịch sử thành công")
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    @GetMapping("/{clubId}/history")
    public ApiResponse<Map<String, Object>> getHistory(
            @PathVariable Long clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.getAttendanceHistory(clubId, date.toString()));
    }

    // ==========================================================
    // ✅ 3. ĐIỂM DANH 1 THÀNH VIÊN + GHI CHÚ
    // ==========================================================
    @Operation(
            summary = "Điểm danh 1 thành viên",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Cập nhật trạng thái điểm danh cho 1 thành viên cụ thể và có thể thêm ghi chú.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Điểm danh thành công")
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark")
    public ApiResponse<String> markAttendance(
            @PathVariable Long sessionId,
            @RequestParam Long membershipId,
            @RequestParam AttendanceStatusEnum status,
            @RequestParam(required = false) String note) {
        attendanceService.markAttendance(sessionId, membershipId, status, note);
        return ApiResponse.msg("Marked successfully");
    }

    // ==========================================================
    // 🔄 4. CẬP NHẬT TRẠNG THÁI ĐIỂM DANH HÀNG LOẠT
    // ==========================================================
    @Operation(
            summary = "Cập nhật trạng thái điểm danh hàng loạt",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Cập nhật cùng một trạng thái (VD: PRESENT/ABSENT) cho toàn bộ thành viên trong session.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Cập nhật hàng loạt thành công")
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark-all")
    public ApiResponse<String> markAll(
            @PathVariable Long sessionId,
            @RequestParam AttendanceStatusEnum status) {
        attendanceService.markAll(sessionId, status);
        return ApiResponse.msg("All attendance updated successfully");
    }

    // ==========================================================
// 👤 5A. THÀNH VIÊN XEM LỊCH SỬ ĐIỂM DANH CỦA CHÍNH MÌNH (TỰ LẤY TỪ JWT)
// ==========================================================
    @Operation(
            summary = "Xem lịch sử điểm danh cá nhân (tự động lấy từ JWT)",
            description = """
            Dành cho **STUDENT** hoặc **CLUB_LEADER**.<br>
            Không cần truyền membershipId.<br>
            Backend tự xác định thành viên từ JWT token và trả về lịch sử điểm danh.
            """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy lịch sử thành công")
    )
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER')")
    @GetMapping("/member/history")
    public ApiResponse<Map<String, Object>> getPersonalMemberHistory(
            @AuthenticationPrincipal CustomUserDetails user) {

        // ✅ Lấy userId từ JWT
        Long userId = user.getUserId();

        // ✅ Tìm Membership đang hoạt động của user
        var membership = membershipRepo.findActiveMembershipsByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new com.example.uniclub.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy membership đang hoạt động của bạn."
                ));

        // ✅ Gọi service cũ để lấy lịch sử theo membershipId
        return ApiResponse.ok(attendanceService.getMemberAttendanceHistory(membership.getMembershipId()));
    }



    // ==========================================================
    // 🏫 6. UNI STAFF XEM TỔNG QUAN ĐIỂM DANH TOÀN TRƯỜNG
    // ==========================================================
    @Operation(
            summary = "Xem tổng quan điểm danh toàn trường",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Có thể lọc theo ngày cụ thể.<br>
                Trả về tỷ lệ điểm danh của tất cả CLB.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lấy tổng quan thành công")
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/university/overview")
    public ApiResponse<Map<String, Object>> getUniversityOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.getUniversityAttendanceOverview(
                date != null ? date.toString() : null
        ));
    }

    // ==========================================================
    // 🆕 7. TẠO BUỔI ĐIỂM DANH MỚI (SESSION)
    // ==========================================================
    @Operation(
            summary = "Tạo buổi điểm danh mới",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Cho phép tạo session điểm danh mới (VD: Buổi sinh hoạt định kỳ).<br>
                Có thể cấu hình ngày, thời gian và mô tả buổi điểm danh.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Tạo session thành công")
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PostMapping("/{clubId}/create-session")
    public ApiResponse<Map<String, Object>> createSession(
            @PathVariable Long clubId,
            @RequestBody ClubAttendanceSessionRequest req) {
        return ApiResponse.ok(attendanceService.createSession(clubId, req));
    }

    // ==========================================================
    // 📦 8. ĐIỂM DANH NHIỀU THÀNH VIÊN CÙNG LÚC
    // ==========================================================
    @Operation(
            summary = "Điểm danh nhiều thành viên cùng lúc",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Gửi danh sách nhiều thành viên và trạng thái tương ứng trong 1 request duy nhất.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Điểm danh hàng loạt thành công")
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark-bulk")
    public ApiResponse<Map<String, Object>> markBulk(
            @PathVariable Long sessionId,
            @RequestBody BulkAttendanceRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.ok(attendanceService.markBulk(sessionId, req, user));
    }
}
