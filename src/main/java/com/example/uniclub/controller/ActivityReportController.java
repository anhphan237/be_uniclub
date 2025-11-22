package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.UpdateBaseScoreRequest;
import com.example.uniclub.dto.response.ClubMonthlyActivitySummaryResponse;
import com.example.uniclub.dto.response.MemberMonthlyActivityResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Activity Report",
        description = """
        API tra cứu **điểm hoạt động hàng tháng** của member / club / toàn hệ thống.<br>
        - Member xem điểm hoạt động của chính mình trong CLB.<br>
        - Leader xem danh sách điểm hoạt động của các member trong CLB.<br>
        - Admin / University Staff xem thống kê toàn trường.<br>
        - Leader xem summary hoạt động của CLB (event, multiplier, member of month, ...).
        """
)
public class ActivityReportController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberMonthlyActivityRepository monthlyActivityRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;

    // ==========================================================
    // A1) MEMBER xem điểm hoạt động của chính mình trong 1 CLB
    // GET /api/clubs/{clubId}/members/me/activity?year=&month=
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/me/activity")
    @Operation(
            summary = "Xem điểm hoạt động của bản thân trong CLB",
            description = """
                    Member xem chi tiết điểm hoạt động của chính mình trong 1 CLB theo tháng.<br>
                    Yêu cầu:<br>
                    - Đã tham gia CLB (ACTIVE hoặc APPROVED).<br>
                    - Tháng đã được hệ thống tính Activity (scheduler hoặc manual).
                    """
    )
    public ResponseEntity<ApiResponse<MemberMonthlyActivityResponse>> getMyMonthlyActivity(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);

        Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubId(
                        current.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (!(membership.getState() == MembershipStateEnum.ACTIVE
                || membership.getState() == MembershipStateEnum.APPROVED)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your membership is not active or approved.");
        }

        MemberMonthlyActivity activity = monthlyActivityRepo
                .findByMembership_MembershipIdAndYearAndMonth(
                        membership.getMembershipId(),
                        year,
                        month
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Monthly activity not found for this membership and month."
                ));

        MemberMonthlyActivityResponse dto = MemberMonthlyActivityResponse.from(activity);

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    // ==========================================================
    // A2) LEADER xem danh sách Activity của toàn bộ member trong CLB
    // GET /api/clubs/{clubId}/members/activity?year=&month=
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/activity")
    @Operation(
            summary = "Leader xem điểm hoạt động của tất cả member trong CLB",
            description = """
                    Leader / Vice-Leader xem danh sách điểm hoạt động của các member trong CLB theo tháng.<br>
                    Kết quả được sort theo <b>finalScore</b> giảm dần (ranking nội bộ CLB).
                    """
    )
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityResponse>>> getClubMembersMonthlyActivity(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        List<MemberMonthlyActivity> list = monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        List<MemberMonthlyActivityResponse> result = list.stream()
                .map(MemberMonthlyActivityResponse::from)
                .sorted(Comparator.comparingDouble(MemberMonthlyActivityResponse::getFinalScore).reversed())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==========================================================
    // A3) ADMIN / UNIVERSITY_STAFF xem toàn trường
    // GET /api/admin/member-activities?year=&month=
    // ==========================================================
    @GetMapping("/admin/member-activities")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(
            summary = "Admin / UniStaff xem toàn bộ activity của member trong 1 tháng",
            description = """
                    Trả về danh sách tất cả MemberMonthlyActivity trong 1 tháng trên toàn hệ thống.<br>
                    Có thể dùng để làm dashboard, thống kê, báo cáo tổng hợp.
                    """
    )
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityResponse>>> getAllMemberActivitiesOfMonth(
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        List<MemberMonthlyActivity> list = monthlyActivityRepo.findByYearAndMonth(year, month);

        List<MemberMonthlyActivityResponse> result = list.stream()
                .map(MemberMonthlyActivityResponse::from)
                .sorted(Comparator.comparingDouble(MemberMonthlyActivityResponse::getFinalScore).reversed())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==========================================================
    // A4) LEADER xem SUMMARY hoạt động CLB trong 1 tháng
    // GET /api/clubs/{clubId}/activity/summary?year=&month=
    // ==========================================================
    @GetMapping("/clubs/{clubId}/activity/summary")
    @Operation(
            summary = "Xem tổng quan hoạt động CLB trong 1 tháng",
            description = """
                    Leader / Vice-Leader xem tổng quan hoạt động của CLB trong một tháng:<br>
                    - Số event đã tổ chức (COMPLETED).<br>
                    - Số member đạt mức FULL.<br>
                    - Member of the month (nếu có).<br>
                    - Hệ số nhân (clubMultiplier) của CLB.
                    """
    )
    public ResponseEntity<ApiResponse<ClubMonthlyActivitySummaryResponse>> getClubMonthlySummary(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate endInclusive = start.plusMonths(1).minusDays(1);

        // 1) Đếm số event COMPLETED trong tháng
        int totalEvents = eventRepo.countByHostClub_ClubIdAndStatusAndDateBetween(
                clubId,
                EventStatusEnum.COMPLETED,
                start,
                endInclusive
        );

        // 2) Lấy list activity của tất cả member trong CLB
        List<MemberMonthlyActivity> activities = monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        long fullCount = activities.stream()
                .filter(a -> a.getActivityLevel() == MemberActivityLevelEnum.FULL)
                .count();

        MemberMonthlyActivity memberOfMonthEntity = activities.stream()
                .filter(a -> a.getActivityLevel() == MemberActivityLevelEnum.MEMBER_OF_MONTH)
                .findFirst()
                .orElse(null);

        MemberMonthlyActivityResponse memberOfMonth = memberOfMonthEntity != null
                ? MemberMonthlyActivityResponse.from(memberOfMonthEntity)
                : null;

        long memberCount = activities.size();

        ClubMonthlyActivitySummaryResponse summary = ClubMonthlyActivitySummaryResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .year(year)
                .month(month)
                .totalEventsCompleted(totalEvents)
                .memberCount(memberCount)
                .fullMembersCount(fullCount)
                .memberOfMonth(memberOfMonth)
                .clubMultiplier(club.getClubMultiplier() != null ? club.getClubMultiplier() : 1.0)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    // ==========================================================
    // Helper: check leader / vice-leader quyền trong CLB
    // ==========================================================
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

    @PutMapping("/clubs/{clubId}/members/activity/base-score")
    @Operation(summary = "Leader nhập điểm phát (baseScore) cho member")
    public ResponseEntity<ApiResponse<Void>> updateBaseScore(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestBody @Valid UpdateBaseScoreRequest req,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        MemberMonthlyActivity act = monthlyActivityRepo
                .findByMembership_MembershipIdAndYearAndMonth(req.getMembershipId(), year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Monthly activity not found"));

        act.setBaseScore(req.getBaseScore());
        monthlyActivityRepo.save(act);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/clubs/{clubId}/members/activity/calculate")
    @Operation(summary = "Tính toán finalScore cho toàn bộ member trong tháng")
    public ResponseEntity<ApiResponse<Void>> calculateMonthlyFinalScore(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        User current = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(current, clubId);

        List<MemberMonthlyActivity> list = monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        for (MemberMonthlyActivity m : list) {

            // Tính attendance score
            double attendanceRate =
                    safeRate(m.getTotalClubPresent(), m.getTotalClubSessions());
            double attendanceScore = attendanceRate * 100;

            // Tính staff score
            double staffScore = switch (m.getStaffEvaluation()) {
                case POOR -> 10;
                case AVERAGE -> 20;
                case GOOD -> 30;
                case EXCELLENT -> 40;
                default -> 0;
            };

            // Multiplier từ enum
            double attendanceMul = ActivityMultiplierEnum.SESSION_ATTENDANCE.value;
            double staffMul = ActivityMultiplierEnum.STAFF_EVALUATION.value;

            double finalScore = m.getBaseScore()
                    + attendanceScore * attendanceMul
                    + staffScore * staffMul;

            m.setAppliedMultiplier(attendanceMul + staffMul);
            m.setFinalScore(finalScore);
        }

        monthlyActivityRepo.saveAll(list);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    private double safeRate(int a, int b) {
        return b == 0 ? 0 : (a * 1.0 / b);
    }

}
