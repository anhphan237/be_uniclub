package com.example.uniclub.controller;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.UniversityService;
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

    // ===============================================================
    // 🔹 Thống kê tổng hợp cấp trường & CLB
    // ===============================================================

    @GetMapping("/statistics")
    public ResponseEntity<UniversityStatisticsResponse> getUniversityStatistics() {
        return ResponseEntity.ok(universityService.getUniversitySummary());
    }

    @GetMapping("/statistics/{clubId}")
    public ResponseEntity<ClubStatisticsResponse> getClubStatistics(@PathVariable Long clubId) {
        return ResponseEntity.ok(universityService.getClubSummary(clubId));
    }

    @GetMapping("/points")
    public ResponseEntity<UniversityPointsResponse> getPointsOverview() {
        return ResponseEntity.ok(universityService.getPointsRanking());
    }

    // ===============================================================
    // 🔹 Attendance overview
    // ===============================================================

    @GetMapping("/attendance-ranking")
    public ResponseEntity<UniversityAttendanceResponse> getAttendanceRanking() {
        return ResponseEntity.ok(universityService.getAttendanceRanking());
    }

    @GetMapping("/attendance-summary")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummary(year));
    }

    @GetMapping("/attendance-summary/club/{clubId}")
    public ResponseEntity<AttendanceSummaryResponse> getClubAttendanceSummary(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByClub(year, clubId));
    }

    @GetMapping("/attendance-summary/event/{eventId}")
    public ResponseEntity<AttendanceSummaryResponse> getEventAttendanceSummary(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "2025") int year
    ) {
        return ResponseEntity.ok(universityService.getAttendanceSummaryByEvent(year, eventId));
    }

    // ===============================================================
    // 🔹 Xem CLB hoạt động sôi nổi theo tháng hoặc năm
    // ===============================================================

    @GetMapping("/stats/clubs")
    public ResponseEntity<List<Map<String, Object>>> getClubActivityStats(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        LocalDate start = (month != null)
                ? LocalDate.of(year, month, 1)
                : LocalDate.of(year, 1, 1);
        LocalDate end = (month != null)
                ? start.withDayOfMonth(start.lengthOfMonth())
                : LocalDate.of(year, 12, 31);

        LocalDate today = LocalDate.now();
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end.isAfter(today) ? today : end;

        List<Club> clubs = clubRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Club c : clubs) {
            long eventCount = eventRepo.findByHostClub_ClubId(c.getClubId()).stream()
                    .filter(e -> e.getDate() != null &&
                            !e.getDate().isBefore(finalStart) &&
                            !e.getDate().isAfter(finalEnd))
                    .count();

            Map<String, Object> clubStats = new LinkedHashMap<>();
            clubStats.put("clubId", c.getClubId());
            clubStats.put("clubName", c.getName());
            clubStats.put("eventCount", eventCount);
            clubStats.put("activityStatus", c.getActivityStatus() != null ? c.getActivityStatus().name() : "UNKNOWN");
            clubStats.put("multiplier", c.getClubMultiplier());
            result.add(clubStats);
        }

        result.sort((a, b) -> Long.compare((long) b.get("eventCount"), (long) a.get("eventCount")));
        return ResponseEntity.ok(result);
    }

    // ===============================================================
    // 🔹 Xem thành viên hoạt động tích cực theo tháng hoặc năm
    // ===============================================================

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
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end.isAfter(today) ? today : end;

        List<Membership> members = membershipRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Membership m : members) {
            if (m.getUser() == null || m.getClub() == null) continue;

            long attendedEvents = regRepo.countByUser_UserIdAndRegisteredAtBetween(
                    m.getUser().getUserId(),
                    finalStart.atStartOfDay(),
                    finalEnd.atTime(23, 59, 59)
            );

            Map<String, Object> memberStats = new LinkedHashMap<>();
            memberStats.put("memberId", m.getMembershipId());
            memberStats.put("memberName", m.getUser().getFullName());
            memberStats.put("clubName", m.getClub().getName());
            memberStats.put("eventCount", attendedEvents);
            memberStats.put("memberLevel", m.getMemberLevel() != null ? m.getMemberLevel().name() : "BASIC");
            memberStats.put("multiplier", m.getMemberMultiplier());
            result.add(memberStats);
        }

        result.sort((a, b) -> Long.compare((long) b.get("eventCount"), (long) a.get("eventCount")));
        return ResponseEntity.ok(result);
    }

    // ===============================================================
    // 📆 1️⃣ CLB hoạt động trong khoảng thời gian tùy chọn
    // ===============================================================
    @GetMapping("/stats/clubs/range")
    public ResponseEntity<List<Map<String, Object>>> getClubActivityRange(
            @RequestParam int fromYear,
            @RequestParam int fromMonth,
            @RequestParam int toYear,
            @RequestParam int toMonth) {

        LocalDate start = LocalDate.of(fromYear, fromMonth, 1);
        LocalDate end = LocalDate.of(toYear, toMonth, 1)
                .withDayOfMonth(LocalDate.of(toYear, toMonth, 1).lengthOfMonth());

        LocalDate today = LocalDate.now();
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end.isAfter(today) ? today : end;

        final LocalDate SYSTEM_LAUNCH_DATE = LocalDate.of(2025, 9, 1);
        if (finalEnd.isBefore(SYSTEM_LAUNCH_DATE)) {
            return ResponseEntity.ok(List.of(Map.of("message", "⚠️ Hệ thống chưa hoạt động trong giai đoạn này (trước tháng 9/2025).")));
        }

        List<Club> clubs = clubRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Club c : clubs) {
            long eventCount = eventRepo.findByHostClub_ClubId(c.getClubId()).stream()
                    .filter(e -> e.getDate() != null &&
                            !e.getDate().isBefore(finalStart) &&
                            !e.getDate().isAfter(finalEnd))
                    .count();

            if (eventCount > 0) {
                Map<String, Object> clubStats = new LinkedHashMap<>();
                clubStats.put("clubId", c.getClubId());
                clubStats.put("clubName", c.getName());
                clubStats.put("eventCount", eventCount);
                clubStats.put("activityStatus", c.getActivityStatus() != null ? c.getActivityStatus().name() : "UNKNOWN");
                clubStats.put("multiplier", c.getClubMultiplier());
                result.add(clubStats);
            }
        }

        if (result.isEmpty()) {
            return ResponseEntity.ok(List.of(Map.of("message", "❌ Không có hoạt động nào trong khoảng thời gian được chọn.")));
        }

        result.sort((a, b) -> Long.compare((long) b.get("eventCount"), (long) a.get("eventCount")));
        return ResponseEntity.ok(result);
    }

    // ===============================================================
    // 📊 2️⃣ Thành viên hoạt động trong khoảng thời gian tùy chọn
    // ===============================================================
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
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end.isAfter(today) ? today : end;

        final LocalDate SYSTEM_LAUNCH_DATE = LocalDate.of(2025, 9, 1);
        if (finalEnd.isBefore(SYSTEM_LAUNCH_DATE)) {
            return ResponseEntity.ok(List.of(Map.of("message", "⚠️ Hệ thống chưa hoạt động trong giai đoạn này (trước tháng 9/2025).")));
        }

        List<Membership> members = membershipRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Membership m : members) {
            if (m.getUser() == null || m.getClub() == null) continue;

            long attendedEvents = regRepo.countByUser_UserIdAndRegisteredAtBetween(
                    m.getUser().getUserId(),
                    finalStart.atStartOfDay(),
                    finalEnd.atTime(23, 59, 59)
            );

            if (attendedEvents > 0) {
                Map<String, Object> memberStats = new LinkedHashMap<>();
                memberStats.put("memberId", m.getMembershipId());
                memberStats.put("memberName", m.getUser().getFullName());
                memberStats.put("clubName", m.getClub().getName());
                memberStats.put("eventCount", attendedEvents);
                memberStats.put("memberLevel", m.getMemberLevel() != null ? m.getMemberLevel().name() : "BASIC");
                memberStats.put("multiplier", m.getMemberMultiplier());
                result.add(memberStats);
            }
        }

        if (result.isEmpty()) {
            return ResponseEntity.ok(List.of(Map.of("message", "❌ Không có thành viên nào hoạt động trong khoảng thời gian được chọn.")));
        }

        result.sort((a, b) -> Long.compare((long) b.get("eventCount"), (long) a.get("eventCount")));
        return ResponseEntity.ok(result);
    }
}
