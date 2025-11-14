package com.example.uniclub.controller;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.UniversityService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 🎓 UniversityController
 * Controller quản lý thống kê toàn trường, CLB, thành viên, điểm và attendance cho UniStaff.
 */
@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final MembershipRepository membershipRepo;
    private final EventRegistrationRepository regRepo;
    private final MemberMonthlyActivityRepository activityRepo;

    // ===============================================================
    // 🔹 Thống kê tổng hợp cấp trường & CLB
    // ===============================================================

    @Operation(summary = "Thống kê tổng hợp toàn trường")
    @GetMapping("/statistics")
    public ResponseEntity<UniversityStatisticsResponse> getUniversityStatistics() {
        return ResponseEntity.ok(universityService.getUniversitySummary());
    }

    @Operation(summary = "Thống kê chi tiết theo CLB")
    @GetMapping("/statistics/{clubId}")
    public ResponseEntity<ClubStatisticsResponse> getClubStatistics(@PathVariable Long clubId) {
        return ResponseEntity.ok(universityService.getClubSummary(clubId));
    }

    @Operation(summary = "Xếp hạng điểm toàn trường")
    @GetMapping("/points")
    public ResponseEntity<UniversityPointsResponse> getPointsOverview() {
        return ResponseEntity.ok(universityService.getPointsRanking());
    }

    // ===============================================================
    // 🔹 Attendance overview
    // ===============================================================

    @Operation(summary = "Xếp hạng attendance toàn trường")
    @GetMapping("/attendance-ranking")
    public ResponseEntity<UniversityAttendanceResponse> getAttendanceRanking() {
        return ResponseEntity.ok(universityService.getAttendanceRanking());
    }

    @Operation(summary = "Tổng hợp attendance theo năm")
    @GetMapping("/attendance-summary")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummary(year));
    }

    @Operation(summary = "Tổng hợp attendance theo CLB")
    @GetMapping("/attendance-summary/club/{clubId}")
    public ResponseEntity<AttendanceSummaryResponse> getClubAttendanceSummary(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByClub(year, clubId));
    }

    @Operation(summary = "Tổng hợp attendance theo sự kiện")
    @GetMapping("/attendance-summary/event/{eventId}")
    public ResponseEntity<AttendanceSummaryResponse> getEventAttendanceSummary(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByEvent(year, eventId));
    }


    // ===============================================================
    // 🔹 Thành viên hoạt động sôi nổi theo tháng (ActivityEngine)
    // ===============================================================

    @Operation(summary = "Thống kê hoạt động thành viên theo tháng/năm")
    @GetMapping("/stats/members")
    public ResponseEntity<List<Map<String, Object>>> getMemberActivityStats(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        LocalDate start = (month != null)
                ? LocalDate.of(year, month, 1)
                : LocalDate.of(year, 1, 1);

        LocalDate end = (month != null)
                ? start.withDayOfMonth(start.lengthOfMonth())
                : LocalDate.of(year, 12, 31);

        LocalDate today = LocalDate.now();
        LocalDate finalEnd = end.isAfter(today) ? today : end;

        List<Membership> members = membershipRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Membership m : members) {
            if (m.getUser() == null || m.getClub() == null) continue;

            long eventCount = regRepo.countByUser_UserIdAndRegisteredAtBetween(
                    m.getUser().getUserId(),
                    start.atStartOfDay(),
                    finalEnd.atTime(23, 59, 59)
            );

            // Lấy activity theo tháng (nếu month != null)
            MemberMonthlyActivity activity = (month != null)
                    ? activityRepo.findByMembership_MembershipIdAndYearAndMonth(
                    m.getMembershipId(), year, month
            ).orElse(null)
                    : null;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("membershipId", m.getMembershipId());
            map.put("memberName", m.getUser().getFullName());
            map.put("clubName", m.getClub().getName());
            map.put("eventCount", eventCount);

            map.put("activityLevel",
                    activity != null ? activity.getActivityLevel().name() : "UNKNOWN");

            map.put("multiplier",
                    activity != null ? activity.getAppliedMultiplier() : m.getMemberMultiplier());

            result.add(map);
        }

        result.sort((a, b) ->
                Long.compare((Long) b.get("eventCount"), (Long) a.get("eventCount"))
        );

        return ResponseEntity.ok(result);
    }


    // ===============================================================
    // 🔹 Thành viên hoạt động theo RANGE (ActivityEngine)
    // ===============================================================

    @Operation(summary = "Thống kê thành viên hoạt động theo range")
    @GetMapping("/stats/members/range")
    public ResponseEntity<List<Map<String, Object>>> getMemberActivityRange(
            @RequestParam int fromYear,
            @RequestParam int fromMonth,
            @RequestParam int toYear,
            @RequestParam int toMonth) {

        LocalDate start = LocalDate.of(fromYear, fromMonth, 1);
        LocalDate end = LocalDate.of(toYear, toMonth, 1)
                .withDayOfMonth(LocalDate.of(toYear, toMonth, 1).lengthOfMonth());

        LocalDate today = LocalDate.now();
        if (end.isAfter(today)) end = today;

        List<Membership> members = membershipRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Membership m : members) {
            if (m.getUser() == null || m.getClub() == null) continue;

            long eventCount = regRepo.countByUser_UserIdAndRegisteredAtBetween(
                    m.getUser().getUserId(),
                    start.atStartOfDay(),
                    end.atTime(23, 59, 59)
            );

            if (eventCount == 0) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("membershipId", m.getMembershipId());
            map.put("memberName", m.getUser().getFullName());
            map.put("clubName", m.getClub().getName());
            map.put("eventCount", eventCount);

            // RANGE không thể xác định activityLevel → UNKNOWN
            map.put("activityLevel", "UNKNOWN");

            map.put("multiplier", m.getMemberMultiplier());

            result.add(map);
        }

        result.sort((a, b) ->
                Long.compare((Long) b.get("eventCount"), (Long) a.get("eventCount"))
        );

        return ResponseEntity.ok(result);
    }
}
