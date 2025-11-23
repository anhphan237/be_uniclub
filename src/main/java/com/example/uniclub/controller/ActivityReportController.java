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
        API tra cứu **điểm hoạt động hàng tháng** của member / club.<br>
        - Member xem điểm hoạt động của chính mình trong CLB.<br>
        - Leader xem danh sách điểm hoạt động của các member trong CLB.<br>
        - Admin / UniStaff xem tổng hợp toàn hệ thống.<br>
        """
)
public class ActivityReportController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberMonthlyActivityRepository monthlyActivityRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;


    // ==========================================================
    // A1) MEMBER xem điểm hoạt động của bản thân trong CLB
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/me/activity")
    @Operation(summary = "Member xem điểm hoạt động của bản thân trong CLB")
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

        return ResponseEntity.ok(ApiResponse.ok(
                MemberMonthlyActivityResponse.from(activity)
        ));
    }

    // ==========================================================
    // A2) LEADER xem danh sách điểm của toàn bộ member
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/activity")
    @Operation(summary = "Leader xem điểm hoạt động của tất cả member trong CLB")
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityResponse>>> getClubMembersMonthlyActivity(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        ensureLeaderRights(jwtUtil.getUserFromRequest(request), clubId);

        List<MemberMonthlyActivity> list = monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        List<MemberMonthlyActivityResponse> result = list.stream()
                .map(MemberMonthlyActivityResponse::from)
                .sorted(Comparator.comparingDouble(MemberMonthlyActivityResponse::getFinalScore).reversed())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==========================================================
    // A3) ADMIN / UNISTAFF xem toàn bộ system
    // ==========================================================
    @GetMapping("/admin/member-activities")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(summary = "Admin / UniStaff xem toàn hệ thống trong một tháng")
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
    // A4) LEADER xem summary hoạt động CLB (Excel Model)
    // ==========================================================
    @GetMapping("/clubs/{clubId}/activity/summary")
    @Operation(summary = "Leader xem tổng quan hoạt động CLB trong một tháng")
    public ResponseEntity<ApiResponse<ClubMonthlyActivitySummaryResponse>> getClubMonthlySummary(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request
    ) {
        ensureLeaderRights(jwtUtil.getUserFromRequest(request), clubId);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        // (1) Số event COMPLETED trong tháng
        int totalEvents = eventRepo.countByHostClub_ClubIdAndStatusAndDateBetween(
                clubId,
                EventStatusEnum.COMPLETED,
                start,
                end
        );

        // (2) List activity của member
        List<MemberMonthlyActivity> activities =
                monthlyActivityRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        // Excel model KHÔNG còn FULL LEVEL + MEMBER OF MONTH + CLUB MULTIPLIER
        long fullCount = 0;
        MemberMonthlyActivityResponse memberOfMonth = null;

        long memberCount = activities.size();

        ClubMonthlyActivitySummaryResponse summary = ClubMonthlyActivitySummaryResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .year(year)
                .month(month)
                .totalEventsCompleted(totalEvents)
                .memberCount(memberCount)
                .fullMembersCount(fullCount)        // fixed = 0
                .memberOfMonth(memberOfMonth)       // always null
                .clubMultiplier(1.0)                // Excel model fixed = 1.0
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }




    // ==========================================================
    // Helper
    // ==========================================================
    private void ensureLeaderRights(User user, Long clubId) {
        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (!(membership.getClubRole() == ClubRoleEnum.LEADER
                || membership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only leader or vice-leader can perform this action.");
        }

        if (!(membership.getState() == MembershipStateEnum.ACTIVE
                || membership.getState() == MembershipStateEnum.APPROVED)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your membership is not active.");
        }
    }
}
