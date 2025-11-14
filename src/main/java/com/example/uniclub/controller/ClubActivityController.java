package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.ClubActivityMonthlyResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Club Activity", description = "Xem và tính toán mức độ hoạt động của member trong CLB")
public class ClubActivityController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberActivityService memberActivityService;
    private final MemberActivityQueryService memberActivityQueryService;

    // ==================== LEADER: Xem activity của member trong CLB ====================

    @GetMapping("/{clubId}/activities/monthly")
    @Operation(
            summary = "Xem hoạt động của tất cả member trong CLB theo tháng",
            description = "Dùng cho CLUB_LEADER / VICE_LEADER để xem mức độ hoạt động & hệ số multiplier của member"
    )
    public ResponseEntity<ApiResponse<ClubActivityMonthlyResponse>> getClubActivityMonthly(
            @PathVariable Long clubId,
            @RequestParam(required = false) String month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(month);

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
            @RequestParam(required = false) String month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        Long clubId = membership.getClub().getClubId();
        ensureClubManagePermission(current, clubId);

        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(month);

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
            @RequestParam(required = false) String month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now().minusMonths(1)   // mặc định tính cho tháng trước
                : YearMonth.parse(month);

        memberActivityService.recalculateForClubAndMonth(clubId, ym);

        String msg = "Recalculated activity for club " + clubId + " in " + ym;
        return ResponseEntity.ok(ApiResponse.ok(msg));
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

        // Kiểm tra user có đúng là leader/vice của clubId không
        Membership m = membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!(m.getClubRole() == ClubRoleEnum.LEADER || m.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not leader/vice-leader of this club.");
        }

        if (!(m.getState() == MembershipStateEnum.ACTIVE || m.getState() == MembershipStateEnum.APPROVED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your membership is not active.");
        }
    }
}
