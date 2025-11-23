package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.ClubActivityMonthlyResponse;
import com.example.uniclub.dto.response.ClubEventMonthlyActivityResponse;
import com.example.uniclub.dto.response.MemberActivityDetailResponse;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.MemberActivityQueryService;
import com.example.uniclub.service.MemberActivityService;
import com.example.uniclub.service.impl.ClubEventActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Club Activity", description = "Xem và tính toán mức độ hoạt động của member trong CLB")
public class ClubActivityController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberActivityService memberActivityService;
    private final ClubEventActivityService clubEventActivityService;
    private final MemberActivityQueryService memberActivityQueryService;

    // ==================== HELPER: build YearMonth ====================

    private YearMonth parseYearMonth(Integer year, Integer month, boolean defaultPrevMonth) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }

        YearMonth now = YearMonth.now();
        return defaultPrevMonth ? now.minusMonths(1) : now;
    }

    // ==================== LEADER: Xem activity của member trong CLB ====================

    @GetMapping("/{clubId}/activities/monthly")
    @Operation(
            summary = "Xem hoạt động của tất cả member trong CLB theo tháng",
            description = "Dùng cho CLUB_LEADER / VICE_LEADER để xem mức độ hoạt động & hệ số multiplier của member"
    )
    public ResponseEntity<ApiResponse<ClubActivityMonthlyResponse>> getClubActivityMonthly(
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = parseYearMonth(year, month, false);

        ClubActivityMonthlyResponse data = memberActivityQueryService.getClubActivity(clubId, ym);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ==================== LEADER: Xem chi tiết 1 member ====================

    @GetMapping("/memberships/{membershipId}/activity")
    @Operation(
            summary = "Xem chi tiết hoạt động của 1 member trong tháng",
            description = "Dùng cho CLUB_LEADER / VICE_LEADER"
    )
    public ResponseEntity<ApiResponse<MemberActivityDetailResponse>> getMemberActivityDetail(
            @PathVariable Long membershipId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = membership.getClub().getClubId();
        ensureClubManagePermission(current, clubId);

        YearMonth ym = parseYearMonth(year, month, false);

        MemberActivityDetailResponse data =
                memberActivityQueryService.getMemberActivity(membershipId, ym);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ==================== LEADER: Tính lại activity cho CLB ====================

    @PostMapping("/{clubId}/activities/recalculate")
    @Operation(
            summary = "Tính lại mức độ hoạt động của member trong CLB cho 1 tháng",
            description = "Cho phép Leader tự tính lại nếu có thay đổi attendance / penalty / staff performance"
    )
    public ResponseEntity<ApiResponse<String>> recalculateClubActivity(
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = parseYearMonth(year, month, true); // default = tháng trước

        memberActivityService.recalculateForClubAndMonth(clubId, ym);

        return ResponseEntity.ok(ApiResponse.ok(
                "Recalculated activity for club " + clubId + " in " + ym
        ));
    }

    // ==================== HELPER: check quyền ====================

    private void ensureClubManagePermission(User user, Long clubId) {
        String roleName = user.getRole().getRoleName();

        boolean isAdminOrStaff = "ADMIN".equalsIgnoreCase(roleName)
                || "UNIVERSITY_STAFF".equalsIgnoreCase(roleName);
        if (isAdminOrStaff) return;

        boolean isLeaderOrVice = "CLUB_LEADER".equalsIgnoreCase(roleName)
                || "VICE_LEADER".equalsIgnoreCase(roleName);
        if (!isLeaderOrVice) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission for this club.");
        }

        Membership m = membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!(m.getClubRole() == ClubRoleEnum.LEADER || m.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not leader/vice-leader of this club.");
        }

        if (!(m.getState() == MembershipStateEnum.ACTIVE || m.getState() == MembershipStateEnum.APPROVED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your membership is not active.");
        }
    }

    // ==================== UNISTAFF: Xem Event Activity ====================

    @GetMapping("/{clubId}/event-activity/monthly")
    @Operation(
            summary = "UniStaff xem hoạt động event của CLB trong 1 tháng",
            description = """
                Bao gồm tổng event, completed, rejected và multiplier CLUB_EVENT_ACTIVITY.
                Dùng cho UNIVERSITY_STAFF hoặc ADMIN.
                """
    )
    public ResponseEntity<ApiResponse<ClubEventMonthlyActivityResponse>> getClubEventActivity(
            @PathVariable Long clubId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);

        String roleName = current.getRole().getRoleName();
        boolean allowed = "ADMIN".equalsIgnoreCase(roleName)
                || "UNIVERSITY_STAFF".equalsIgnoreCase(roleName);

        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only UniStaff/Admin can view club event activity.");
        }

        YearMonth ym = parseYearMonth(year, month, false);

        ClubEventMonthlyActivityResponse data = clubEventActivityService.getClubEventActivity(clubId, ym);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

}
