package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CreateClubPenaltyRequest;
import com.example.uniclub.dto.request.StaffPerformanceRequest;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.PenaltyRuleRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.ClubPenaltyService;
import com.example.uniclub.service.StaffPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/{clubId}/discipline")
@RequiredArgsConstructor
@Tag(
        name = "Quản lý kỷ luật & đánh giá Staff trong CLB",
        description = """
        Các API dùng để quản lý kỷ luật thành viên và chấm điểm hiệu suất làm việc của staff trong sự kiện.<br>
        Chỉ <b>CLUB_LEADER</b> và <b>VICE_LEADER</b> mới có quyền truy cập.<br><br>
        Bao gồm:<br>
        - Tạo phiếu phạt cho thành viên (vắng mặt, đi trễ, vi phạm nội quy, gian lận...).<br>
        - Chấm điểm hiệu suất làm staff cho từng sự kiện (POOR / AVERAGE / GOOD / EXCELLENT).<br>
        - Dữ liệu phạt và hiệu suất sẽ được hệ thống dùng để tính điểm hoạt động hằng tháng.
        """
)

public class ClubDisciplineController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final ClubPenaltyService clubPenaltyService;
    private final StaffPerformanceService staffPerformanceService;
    private final PenaltyRuleRepository ruleRepo;

    // ============================================================================
    // 1) CREATE MEMBER PENALTY
    // ============================================================================
    @PostMapping("/penalties")
    @Operation(
            summary = "Tạo phiếu phạt cho thành viên trong CLB",
            description = """
                Leader / Vice-leader dùng API này để tạo phiếu phạt cho thành viên CLB.<br><br>
                Một số loại vi phạm thường gặp:<br>
                - Vắng mặt không báo trước<br>
                - Đi trễ sự kiện / buổi họp<br>
                - Không hoàn thành nhiệm vụ staff<br>
                - Vi phạm nội quy, hành vi không phù hợp<br>
                - Gian lận (check-in hộ, khai sai...)<br><br>
                Hệ thống sẽ tự tổng hợp điểm phạt vào cuối tháng để tính mức độ hoạt động.
                """
    )

    public ResponseEntity<ApiResponse<?>> createPenalty(
            @PathVariable Long clubId,
            HttpServletRequest request,
            @Valid @RequestBody CreateClubPenaltyRequest body
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        var penalty = clubPenaltyService.createPenalty(clubId, body, current);

        // FIX: ApiResponse only accepts one parameter
        return ResponseEntity.ok(
                ApiResponse.ok("Penalty created successfully.")
        );
    }

    // ============================================================================
    // 2) CREATE STAFF PERFORMANCE RECORD
    // ============================================================================
    @PostMapping("/staff-performances")
    @Operation(
            summary = "Chấm điểm hiệu suất làm việc của Staff trong sự kiện",
            description = """
                Leader / Vice-leader đánh giá mức độ hoàn thành nhiệm vụ của staff trong từng sự kiện.<br><br>
                Các mức đánh giá:<br>
                - POOR (0.0): Làm việc kém, không hoàn thành nhiệm vụ<br>
                - AVERAGE (0.4): Hoàn thành một phần nhưng còn thiếu<br>
                - GOOD (0.8): Hoàn thành đầy đủ nhiệm vụ được giao<br>
                - EXCELLENT (1.0): Làm việc xuất sắc, vượt mong đợi<br><br>
                Điểm staff sẽ được hệ thống tính vào tổng điểm hoạt động tháng.
                """
    )

    public ResponseEntity<ApiResponse<?>> createStaffPerformance(
            @PathVariable Long clubId,
            HttpServletRequest request,
            @Valid @RequestBody StaffPerformanceRequest body
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        var perf = staffPerformanceService.createStaffPerformance(clubId, body, current);

        return ResponseEntity.ok(
                ApiResponse.ok("Staff performance saved successfully.")
        );
    }

    @GetMapping("/penalty-rules")
    @Operation(
            summary = "Danh sách rule vi phạm cho Leader/Vice-Leader",
            description = """
            Leader / Vice-leader dùng API này để xem danh sách Rule đã được UniStaff cấu hình.<br><br>
            Leader sẽ chọn ruleId từ danh sách này khi tạo phiếu phạt.<br>
            Không có quyền chỉnh sửa rule.
            """
    )
    public ResponseEntity<ApiResponse<?>> listPenaltyRules(
            @PathVariable Long clubId,
            HttpServletRequest request
    ) {
        // check quyền leader/vice
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        // chỉ đọc rule, không lọc theo club (rule là global)
        return ResponseEntity.ok(
                ApiResponse.ok(ruleRepo.findAll())
        );
    }

    // ============================================================================
    // Helper: check leader permission
    // ============================================================================
    private void ensureLeaderRights(User user, Long clubId) {

        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!(membership.getClubRole() == ClubRoleEnum.LEADER ||
                membership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only leader or vice-leader can perform this action.");
        }

        if (!(membership.getState() == MembershipStateEnum.ACTIVE ||
                membership.getState() == MembershipStateEnum.APPROVED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your membership is not active.");
        }
    }
}
