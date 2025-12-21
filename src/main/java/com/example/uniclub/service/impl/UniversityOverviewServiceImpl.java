package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ClubOverviewResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.UniversityOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityOverviewServiceImpl implements UniversityOverviewService {

    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final EventFeedbackRepository feedbackRepo;
    private final MembershipRepository membershipRepo;
    private final EventStaffRepository staffRepo;
    private final WalletTransactionRepository walletRepo;
    private final ProductOrderRepository productOrderRepo;
    private final AttendanceRecordRepository attendanceRepo;

    // ============================================================
    // 🔵 API 1 — Tổng hợp ALL thời gian
    // ============================================================
    @Override
    public List<ClubOverviewResponse> getAllClubOverview() {

        List<Club> allClubs = clubRepo.findAll();

        return allClubs.stream()
                .map(club -> buildClubOverview(club, null, null))
                .sorted(Comparator.comparing(ClubOverviewResponse::ratingEvent).reversed())
                .collect(Collectors.toList());
    }

    // ============================================================
    // 🔵 API 2 — Tổng hợp theo tháng
    // ============================================================
    @Override
    public List<ClubOverviewResponse> getAllClubOverviewByMonth(int year, int month) {

        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<Club> allClubs = clubRepo.findAll();

        return allClubs.stream()
                .map(club -> buildClubOverview(club, start, end))
                .sorted(Comparator.comparing(ClubOverviewResponse::ratingEvent).reversed())
                .collect(Collectors.toList());
    }

    // ============================================================
    // 🔵 CORE LOGIC — Dùng cho cả 2 API
    // ============================================================
    private ClubOverviewResponse buildClubOverview(Club club, LocalDateTime start, LocalDateTime end) {

        Long clubId = club.getClubId();

        // 1️⃣ Lấy danh sách event host + cohost của CLB
        List<Event> events = eventRepo.findByClubParticipation(clubId);

        // Nếu có filter tháng → lọc theo event.startDate
        if (start != null && end != null) {
            events = events.stream()
                    .filter(e -> {
                        LocalDate date = e.getStartDate();
                        LocalDateTime dt = date.atStartOfDay();
                        return !dt.isBefore(start) && dt.isBefore(end);
                    })
                    .collect(Collectors.toList());
        }

        // ------------------------------------------------------------
        // 1. ratingEvent
        Long totalRating = feedbackRepo.getTotalRatingForClub(clubId);
        Long ratingCount = feedbackRepo.getTotalFeedbackCountForClub(clubId);
        Double ratingEvent = (ratingCount == null || ratingCount == 0)
                ? 0.0
                : totalRating * 1.0 / ratingCount;

        // ------------------------------------------------------------
        // 2. totalCheckin & checkinRate
        Long totalCheckin = eventRepo.sumTotalCheckinByClub(clubId);
        Double checkinRate = eventRepo.avgCheckinRateByClub(clubId);
        if (checkinRate == null) checkinRate = 0.0;

        // ------------------------------------------------------------
        // 3. totalMember ACTIVE
        Long totalMember = membershipRepo.countByClub_ClubIdAndState(
                clubId, MembershipStateEnum.ACTIVE
        );

        // ------------------------------------------------------------
        // 4. totalStaff ACTIVE (host+cohost)
        Long totalStaff = staffRepo.countStaffAssignmentsByClub(clubId);

        // ------------------------------------------------------------
        // 5. totalBudgetEvent
        Long totalBudgetEvent = walletRepo.sumEventBudgetByClub(clubId);

        // ------------------------------------------------------------
        // 6. totalProductEvent
        Long totalProductEvent = productOrderRepo.sumEventProductsByClub(clubId);

        // ------------------------------------------------------------
        // 7. totalDiscipline (không có bảng → mặc định 0)
        Long totalDiscipline = 0L;

        // ------------------------------------------------------------
        // 8. attendanceRate (FULL=1, HALF=0.5, NONE=0)
        Double attendanceRate = calculateAttendanceRate(clubId, totalMember, start, end);

        // ------------------------------------------------------------
        return ClubOverviewResponse.builder()
                .clubId(clubId)
                .clubName(club.getName())
                .ratingEvent(ratingEvent)
                .totalCheckin(totalCheckin)
                .checkinRate(checkinRate)
                .totalMember(totalMember)
                .totalStaff(totalStaff)
                .totalBudgetEvent(totalBudgetEvent)
                .totalProductEvent(totalProductEvent)
                .totalDiscipline(totalDiscipline)
                .attendanceRate(attendanceRate)
                .build();
    }


    // ============================================================
    // 🔵 Tính AttendanceRate FULL=1, HALF=0.5, NONE=0
    // ============================================================
    // ============================================================
// 🔵 Tính AttendanceRate
// FULL = 1.0
// HALF = 0.5
// NONE / SUSPICIOUS = 0.0
// NULL (public / legacy) = 1.0
// ============================================================
    private Double calculateAttendanceRate(
            Long clubId,
            Long totalMember,
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (totalMember == null || totalMember == 0) return 0.0;

        List<AttendanceRecord> records;

        if (start == null || end == null) {
            records = attendanceRepo.findAttendanceByClub(clubId);
        } else {
            records = attendanceRepo.findAttendanceByClubAndDateRange(clubId, start, end);
        }

        if (records.isEmpty()) return 0.0;

        // Tổng điểm attendance theo từng event
        Map<Long, Double> eventAttendanceScore = new HashMap<>();

        for (AttendanceRecord ar : records) {

            AttendanceLevelEnum level = ar.getAttendanceLevel();

            double score;
            if (level == null) {
                // ✅ Public event / legacy data / chưa finalize
                score = 1.0;
            } else {
                score = switch (level) {
                    case FULL -> 1.0;
                    case HALF -> 0.5;
                    case NONE, SUSPICIOUS -> 0.0;
                };
            }

            Long eventId = ar.getEvent().getEventId();
            eventAttendanceScore.merge(eventId, score, Double::sum);
        }

        // Tính attendance rate cho từng event
        List<Double> eventRates = eventAttendanceScore.values().stream()
                .map(totalScore -> totalScore / totalMember)
                .toList();

        // Trung bình toàn CLB
        return eventRates.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }


}
