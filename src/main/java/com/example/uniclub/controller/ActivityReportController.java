package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CalculateScoreRequest;
import com.example.uniclub.dto.request.CreateMonthlyActivityRequest;
import com.example.uniclub.dto.request.UpdateMonthlyActivityBulkRequest;
import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.ActivityEngineService;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name="Activity Report", description="Activity tracking & monthly scoring")
public class ActivityReportController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberMonthlyActivityRepository monthlyRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final EventDayRepository eventDayRepo;
    private final ActivityEngineService activityService;

    // ==========================================================
    // Validate Month helper
    // ==========================================================
    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must be between 1–12");

        if (year < 2000)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year");
    }

    // ==========================================================
    // A1) MEMBER xem điểm hoạt động của bản thân trong CLB
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/me/activity")
    @Operation(summary = "Member xem điểm hoạt động của bản thân")
    public ResponseEntity<ApiResponse<MemberMonthlyActivityResponse>> getMyMonthlyActivity(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest req
    ) {
        validateMonth(year, month);

        User user = jwtUtil.getUserFromRequest(req);

        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (membership.getState() != MembershipStateEnum.ACTIVE
                && membership.getState() != MembershipStateEnum.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your membership is not active.");
        }

        MemberMonthlyActivity activity =
                activityService.getMonthlyActivity(membership.getMembershipId(), year, month);

        return ResponseEntity.ok(ApiResponse.ok(
                MemberMonthlyActivityResponse.from(activity)));
    }

    // ==========================================================
    // A2) LEADER xem danh sách member hoạt động
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/activity")
    @Operation(summary = "Leader xem điểm hoạt động của toàn CLB")
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityLiteResponse>>> getClubMonthlyActivities(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest req
    ) {
        validateMonth(year, month);
        User user = jwtUtil.getUserFromRequest(req);
        ensureLeaderRights(user, clubId);

        List<MemberMonthlyActivity> list =
                activityService.getClubMonthlyActivities(clubId, year, month);

        List<MemberMonthlyActivityLiteResponse> result = list.stream()
                .map(MemberMonthlyActivityResponse::toLite) // dùng converter mới
                .sorted(Comparator.comparing(MemberMonthlyActivityLiteResponse::getTotalClubPresent).reversed())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }



    // ==========================================================
    // A3) ADMIN xem toàn system
    // ==========================================================
    @GetMapping("/admin/member-activities")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @Operation(summary = "Admin xem toàn hệ thống theo tháng")
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityResponse>>> getAllActivities(
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        validateMonth(year, month);

        List<MemberMonthlyActivityResponse> result =
                monthlyRepo.findByYearAndMonth(year, month)
                        .stream()
                        .map(MemberMonthlyActivityResponse::from)
                        .sorted(Comparator.comparingDouble(MemberMonthlyActivityResponse::getFinalScore).reversed())
                        .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==========================================================
    // A4) CLUB SUMMARY
    // ==========================================================
    @GetMapping("/clubs/{clubId}/activity/summary")
    @Operation(summary = "Leader xem tổng quan hoạt động CLB")
    public ResponseEntity<ApiResponse<ClubMonthlyActivitySummaryResponse>> getClubSummary(
            @PathVariable Long clubId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest req
    ) {

        validateMonth(year, month);

        User user = jwtUtil.getUserFromRequest(req);
        ensureLeaderRights(user, clubId);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.plusMonths(1).minusDays(1);

        // ⭐ LẤY EVENT THEO THÁNG = DỰA TRÊN EVENT DAY
        List<Long> eventIds = eventDayRepo.findEventIdsByClubAndMonth(clubId, start, end);

        if (eventIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(
                    ClubMonthlyActivitySummaryResponse.builder()
                            .clubId(club.getClubId())
                            .clubName(club.getName())
                            .year(year)
                            .month(month)
                            .totalEventsCompleted(0)
                            .memberCount(0L)
                            .fullMembersCount(0L)
                            .memberOfMonth(null)
                            .clubMultiplier(1.0)
                            .build()
            ));
        }

        List<Event> events = eventRepo.findAllById(eventIds);

        // ⭐ ĐẾM SỐ SỰ KIỆN ĐÃ COMPLETED (tính 1 event duy nhất)
        int totalEventsCompleted = (int) events.stream()
                .filter(e -> e.getStatus() == EventStatusEnum.COMPLETED)
                .count();

        // ⭐ LẤY HOẠT ĐỘNG MEMBER
        List<MemberMonthlyActivity> activities =
                monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        ClubMonthlyActivitySummaryResponse summary = ClubMonthlyActivitySummaryResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .year(year)
                .month(month)
                .totalEventsCompleted(totalEventsCompleted)
                .memberCount((long) activities.size())
                .fullMembersCount(0L)
                .memberOfMonth(null)
                .clubMultiplier(1.0)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }


    // ==========================================================
    // PREVIEW SCORE
    // ==========================================================
    @PostMapping("/clubs/{clubId}/members/{membershipId}/calculate-score")
    @Operation(summary = "Leader preview điểm one member")
    public ResponseEntity<ApiResponse<CalculateScoreResponse>> previewScore(
            @PathVariable Long clubId,
            @PathVariable Long membershipId,
            @RequestBody CalculateScoreRequest req,
            HttpServletRequest http
    ) {
        User user = jwtUtil.getUserFromRequest(http);
        ensureLeaderRights(user, clubId);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Membership does not belong to this club.");
        }

        CalculateScoreResponse resp = activityService.calculatePreviewScore(
                membershipId,
                req.getAttendanceBaseScore(),
                req.getStaffBaseScore()
        );

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    // ==========================================================
    // LIVE SCORE LIST
    // ==========================================================
    @GetMapping("/clubs/{clubId}/members/activity-live")
    @Operation(summary = "Leader xem điểm LIVE real-time của CLB")
    public ResponseEntity<ApiResponse<List<MemberMonthlyActivityResponse>>> getLiveActivity(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "100") int attendanceBase,
            @RequestParam(defaultValue = "100") int staffBase,
            HttpServletRequest request
    ) {
        User user = jwtUtil.getUserFromRequest(request);
        ensureLeaderRights(user, clubId);

        List<MemberMonthlyActivityResponse> result =
                activityService.calculateLiveActivities(clubId, attendanceBase, staffBase);

        return ResponseEntity.ok(ApiResponse.ok(result));

    }

    // ==========================================================
    // PERMISSION CHECK
    // ==========================================================
    private void ensureLeaderRights(User user, Long clubId) {
        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (membership.getClubRole() != ClubRoleEnum.LEADER &&
                membership.getClubRole() != ClubRoleEnum.VICE_LEADER) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only leader or vice-leader can perform this action.");
        }

        if (membership.getState() != MembershipStateEnum.ACTIVE &&
                membership.getState() != MembershipStateEnum.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your membership is not active.");
        }
    }
    @PostMapping("/clubs/{clubId}/activity/monthly/create")
    @Operation(summary = "Leader tạo báo cáo hoạt động tháng cho 1 membership")
    public ResponseEntity<ApiResponse<MemberMonthlyActivityResponse>> createMonthlyActivity(
            @PathVariable Long clubId,
            @RequestBody CreateMonthlyActivityRequest req,
            HttpServletRequest http
    ) {
        User user = jwtUtil.getUserFromRequest(http);
        ensureLeaderRights(user, clubId);

        // Validate month
        validateMonth(req.getYear(), req.getMonth());

        // Khóa tháng cũ
        LocalDate now = LocalDate.now();
        if (req.getYear() < now.getYear() ||
                (req.getYear() == now.getYear() && req.getMonth() < now.getMonthValue())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Past months are locked, cannot create.");
        }

        // Check exists
        MemberMonthlyActivity exist = monthlyRepo
                .findByMembership_MembershipIdAndYearAndMonth(
                        req.getMembershipId(), req.getYear(), req.getMonth())
                .orElse(null);

        if (exist != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Monthly activity already exists.");
        }

        Membership membership = membershipRepo.findById(req.getMembershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Membership not in this club");
        }

        MemberMonthlyActivity entity = new MemberMonthlyActivity();
        applyMonthlyRequest(req, entity);
        entity.setMembership(membership);

        monthlyRepo.save(entity);

        return ResponseEntity.ok(ApiResponse.ok(MemberMonthlyActivityResponse.from(entity)));
    }

    @PutMapping("/clubs/{clubId}/activity/monthly/update-bulk")
    @Operation(summary = "Leader cập nhật báo cáo hoạt động nhiều thành viên cùng lúc")
    public ResponseEntity<ApiResponse<String>> updateBulkMonthlyActivity(
            @PathVariable Long clubId,
            @RequestBody UpdateMonthlyActivityBulkRequest req,
            HttpServletRequest http
    ) {
        User user = jwtUtil.getUserFromRequest(http);
        ensureLeaderRights(user, clubId);

        int year = req.getYear();
        int month = req.getMonth();

        validateMonth(year, month);

        // Khóa tháng cũ
        LocalDate now = LocalDate.now();
        if (year < now.getYear() ||
                (year == now.getYear() && month < now.getMonthValue())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Past months are locked, cannot update.");
        }

        for (CreateMonthlyActivityRequest item : req.getItems()) {

            MemberMonthlyActivity entity = monthlyRepo
                    .findByMembership_MembershipIdAndYearAndMonth(
                            item.getMembershipId(), year, month)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Activity not found for membership " + item.getMembershipId()));

            applyMonthlyRequest(item, entity);
            monthlyRepo.save(entity);
        }

        return ResponseEntity.ok(ApiResponse.ok("Updated successfully"));
    }


    private void applyMonthlyRequest(CreateMonthlyActivityRequest req, MemberMonthlyActivity e) {

        e.setYear(req.getYear());
        e.setMonth(req.getMonth());

        e.setTotalEventRegistered(req.getTotalEventRegistered());
        e.setTotalEventAttended(req.getTotalEventAttended());
        e.setEventAttendanceRate(req.getEventAttendanceRate());

        e.setTotalPenaltyPoints(req.getTotalPenaltyPoints());
        e.setActivityLevel(req.getActivityLevel());

        e.setAttendanceBaseScore(req.getAttendanceBaseScore());
        e.setAttendanceMultiplier(req.getAttendanceMultiplier());
        e.setAttendanceTotalScore(req.getAttendanceTotalScore());

        e.setStaffBaseScore(req.getStaffBaseScore());
        e.setTotalStaffCount(req.getTotalStaffCount());
        e.setStaffEvaluation(req.getStaffEvaluation());
        e.setStaffMultiplier(req.getStaffMultiplier());
        e.setStaffScore(req.getStaffScore());
        e.setStaffTotalScore(req.getStaffTotalScore());

        e.setTotalClubSessions(req.getTotalClubSessions());
        e.setTotalClubPresent(req.getTotalClubPresent());
        e.setSessionAttendanceRate(req.getSessionAttendanceRate());

        e.setFinalScore(req.getFinalScore());
    }


}
