package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ActivityEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEngineServiceImpl implements ActivityEngineService {

    private final MembershipRepository membershipRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final ClubAttendanceRecordRepository clubAttendanceRecordRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;
    private final ClubPenaltyRepository clubPenaltyRepo;
    private final MemberMonthlyActivityRepository monthlyActivityRepo;
    private final MultiplierPolicyRepository multiplierPolicyRepo;
    private final ClubRepository clubRepo;

    // ==========================================================
    // 🔹 PUBLIC API
    // ==========================================================
    @Override
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month) {
        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        MemberMonthlyActivity activity = calculateActivityForMembership(membership, year, month);
        log.info("Recalculated monthly activity for membership {} ({}/{})",
                membershipId, month, year);
        return activity;
    }

    @Override
    @Transactional
    public void recalculateForClub(Long clubId, int year, int month) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        List<Membership> members = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
        );

        if (members.isEmpty()) {
            log.info("No active members in club {} for activity calculation", club.getName());
            return;
        }

        Map<Membership, MemberMonthlyActivity> resultMap = new HashMap<>();
        for (Membership m : members) {
            MemberMonthlyActivity act = calculateActivityForMembership(m, year, month);
            resultMap.put(m, act);
        }

        // 🔹 Xác định MEMBER_OF_MONTH trong CLB (nếu muốn)
        pickMemberOfMonthInClub(clubId, year, month);

        log.info("Recalculated monthly activity for club {} ({}/{}) – {} members",
                club.getName(), month, year, members.size());
    }

    @Override
    @Transactional
    public void recalculateAllForMonth(int year, int month) {
        List<Club> clubs = clubRepo.findAll();
        for (Club club : clubs) {
            recalculateForClub(club.getClubId(), year, month);
        }
        log.info("Recalculated monthly activity for ALL clubs ({}/{})", month, year);
    }

    // ==========================================================
    // 🔹 CORE CALCULATION
    // ==========================================================
    @Transactional
    protected MemberMonthlyActivity calculateActivityForMembership(Membership membership,
                                                                   int year,
                                                                   int month) {
        Long userId = membership.getUser().getUserId();
        Long membershipId = membership.getMembershipId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDateExclusive = startDate.plusMonths(1); // [start, end)

        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDateExclusive.atStartOfDay();

        // 1️⃣ Event participation
        var eventStats = computeEventStats(userId, startDate, endDateExclusive);

        // 2️⃣ Club session attendance
        var sessionStats = computeClubSessionStats(userId, startDt, endDt);

        // 3️⃣ Staff performance
        double avgStaffPerf = computeStaffPerformanceScore(membershipId, startDate, endDateExclusive.minusDays(1));

        // 4️⃣ Penalty
        int totalPenalty = computePenaltyPoints(membershipId, startDt, endDt);

        // 5️⃣ Tính base score (0–1)
        double baseScore = computeBaseScore(
                eventStats,
                sessionStats,
                avgStaffPerf,
                totalPenalty
        );
        int basePercent = (int) Math.round(baseScore * 100);

        // 6️⃣ Lấy MultiplierPolicy để map sang level + multiplier (KHÔNG HARDCODE)
        var multiplierResult = resolveMultiplierFromPolicy(basePercent);

        MemberActivityLevelEnum level = multiplierResult.level;
        double appliedMultiplier = multiplierResult.multiplier;
        double finalScore = baseScore * appliedMultiplier;

        // 7️⃣ Tìm hoặc tạo record monthly
        MemberMonthlyActivity monthly = monthlyActivityRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElseGet(() -> MemberMonthlyActivity.builder()
                        .membership(membership)
                        .year(year)
                        .month(month)
                        .build()
                );

        monthly.setTotalEventRegistered(eventStats.totalRegistered);
        monthly.setTotalEventAttended(eventStats.totalAttendedWeighted);
        monthly.setTotalClubSessions(sessionStats.totalSessions);
        monthly.setTotalClubPresent(sessionStats.presentSessions);
        monthly.setAvgStaffPerformance(avgStaffPerf);
        monthly.setTotalPenaltyPoints(totalPenalty);
        monthly.setBaseScore(baseScore);
        monthly.setBaseScorePercent(basePercent);
        monthly.setActivityLevel(level);
        monthly.setAppliedMultiplier(appliedMultiplier);
        monthly.setFinalScore(finalScore);

        // Cập nhật hệ số vào Membership
        membership.setMemberMultiplier(appliedMultiplier);
        membershipRepo.save(membership);

        return monthlyActivityRepo.save(monthly);
    }

    // ==========================================================
    // 🔹 EVENT STATS
    // ==========================================================
    private static class EventStats {
        int totalRegistered;
        int totalAttendedWeighted;
        double attendanceRatio;
    }

    private EventStats computeEventStats(Long userId, LocalDate start, LocalDate endExclusive) {
        List<EventRegistration> all = eventRegistrationRepo
                .findByUser_UserIdOrderByRegisteredAtDesc(userId);

        List<EventRegistration> inMonth = all.stream()
                .filter(r -> r.getEvent() != null && r.getEvent().getDate() != null)
                .filter(r -> {
                    LocalDate d = r.getEvent().getDate();
                    return !d.isBefore(start) && d.isBefore(endExclusive);
                })
                .toList();

        int total = inMonth.size();
        int attendedWeighted = 0;

        for (EventRegistration r : inMonth) {
            if (r.getAttendanceLevel() == null) continue;
            switch (r.getAttendanceLevel()) {
                case FULL -> attendedWeighted += 1;
                case HALF -> attendedWeighted += 0.5;
                default -> {}
            }
        }

        double ratio = (total == 0) ? 0.0 : (double) attendedWeighted / total;

        EventStats stats = new EventStats();
        stats.totalRegistered = total;
        stats.totalAttendedWeighted = attendedWeighted;
        stats.attendanceRatio = Math.min(1.0, Math.max(0.0, ratio));
        return stats;
    }

    // ==========================================================
    // 🔹 CLUB SESSION STATS
    // ==========================================================
    private static class SessionStats {
        int totalSessions;
        int presentSessions;
        double attendanceRatio;
    }

    private SessionStats computeClubSessionStats(Long userId,
                                                 LocalDateTime startDt,
                                                 LocalDateTime endDt) {

        int totalSessions = clubAttendanceRecordRepo
                .countByMembership_User_UserIdAndSession_CreatedAtBetween(
                        userId, startDt, endDt
                );

        int present = clubAttendanceRecordRepo
                .countByMembership_User_UserIdAndStatusInAndSession_CreatedAtBetween(
                        userId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        startDt,
                        endDt
                );

        double ratio = (totalSessions == 0) ? 0.0 : (double) present / totalSessions;

        SessionStats stats = new SessionStats();
        stats.totalSessions = totalSessions;
        stats.presentSessions = present;
        stats.attendanceRatio = Math.min(1.0, Math.max(0.0, ratio));
        return stats;
    }

    // ==========================================================
    // 🔹 STAFF PERFORMANCE (0–1)
    // ==========================================================
    private double computeStaffPerformanceScore(Long membershipId,
                                                LocalDate start,
                                                LocalDate endInclusive) {
        List<StaffPerformance> list = staffPerformanceRepo
                .findByMembership_MembershipIdAndEvent_DateBetween(
                        membershipId,
                        start,
                        endInclusive
                );

        if (list.isEmpty()) return 0.0;

        double sum = 0.0;
        for (StaffPerformance sp : list) {
            sum += mapPerformanceLevelToScore(sp.getPerformance());
        }
        return Math.min(1.0, Math.max(0.0, sum / list.size()));
    }

    private double mapPerformanceLevelToScore(PerformanceLevelEnum level) {
        if (level == null) return 0.0;
        return switch (level) {
            case POOR -> 0.25;
            case AVERAGE -> 0.5;
            case GOOD -> 0.75;
            case EXCELLENT -> 1.0;
        };
    }

    // ==========================================================
    // 🔹 PENALTY POINTS
    // ==========================================================
    private int computePenaltyPoints(Long membershipId,
                                     LocalDateTime start,
                                     LocalDateTime end) {
        List<ClubPenalty> penalties = clubPenaltyRepo
                .findByMembership_MembershipIdAndCreatedAtBetween(
                        membershipId,
                        start,
                        end
                );

        return penalties.stream()
                .mapToInt(ClubPenalty::getPoints)
                .sum(); // thường là âm
    }

    // ==========================================================
    // 🔹 BASE SCORE (0–1) – CÓ TRỌNG SỐ
    // ==========================================================
    private double computeBaseScore(EventStats eventStats,
                                    SessionStats sessionStats,
                                    double staffScore,
                                    int totalPenaltyPoints) {

        double eventScore = eventStats.attendanceRatio;   // 0–1
        double sessionScore = sessionStats.attendanceRatio; // 0–1
        double staff = staffScore;                        // 0–1

        double penaltyFactor = mapPenaltyToFactor(totalPenaltyPoints);

        // Trọng số có thể điều chỉnh sau nếu muốn
        double baseScore =
                eventScore * 0.5 +
                        sessionScore * 0.2 +
                        staff * 0.2 +
                        penaltyFactor * 0.1;

        return Math.min(1.0, Math.max(0.0, baseScore));
    }

    private double mapPenaltyToFactor(int totalPenaltyPoints) {
        if (totalPenaltyPoints >= 0) return 1.0;       // không bị phạt
        if (totalPenaltyPoints <= -50) return 0.0;
        if (totalPenaltyPoints <= -30) return 0.25;
        if (totalPenaltyPoints <= -15) return 0.5;
        return 0.75; // -15 < points < 0
    }

    // ==========================================================
    // 🔹 RESOLVE MULTIPLIER FROM POLICY (NO HARDCODE RANGE)
    // ==========================================================
    private record MultiplierResult(MemberActivityLevelEnum level, double multiplier) {}

    private MultiplierResult resolveMultiplierFromPolicy(int baseScorePercent) {
        // Lấy policy cho MEMBER activity score
        List<MultiplierPolicy> policies = multiplierPolicyRepo
                .findByTargetTypeAndActivityTypeAndActiveIsTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.MEMBER_ACTIVITY_SCORE  // nhớ thêm enum này
                );

        for (MultiplierPolicy p : policies) {
            Integer min = p.getMinThreshold();
            Integer max = p.getMaxThreshold(); // có thể null = không giới hạn

            boolean okMin = baseScorePercent >= min;
            boolean okMax = (max == null) || (baseScorePercent <= max);

            if (okMin && okMax) {
                MemberActivityLevelEnum level;
                try {
                    level = MemberActivityLevelEnum.valueOf(p.getRuleName());
                } catch (IllegalArgumentException ex) {
                    // fallback nếu ruleName không khớp enum
                    level = MemberActivityLevelEnum.NORMAL;
                }
                return new MultiplierResult(level, p.getMultiplier());
            }
        }

        // Không tìm thấy policy phù hợp -> fallback
        return new MultiplierResult(MemberActivityLevelEnum.NORMAL, 1.0);
    }

    // ==========================================================
    // 🔹 MEMBER OF MONTH (trong từng CLB)
    // ==========================================================
    @Transactional
    protected void pickMemberOfMonthInClub(Long clubId, int year, int month) {
        List<MemberMonthlyActivity> list = monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        if (list.isEmpty()) return;

        // tìm người có finalScore cao nhất
        MemberMonthlyActivity best = list.stream()
                .max(Comparator.comparingDouble(MemberMonthlyActivity::getFinalScore))
                .orElse(null);

        if (best == null) return;

        // chỉ set MEMBER_OF_MONTH nếu điểm đủ cao (vd >= 0.8 base)
        if (best.getBaseScore() < 0.8) return;

        // lấy lại multiplier từ policy cho level MEMBER_OF_MONTH
        List<MultiplierPolicy> policies = multiplierPolicyRepo
                .findByTargetTypeAndActivityTypeAndActiveIsTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.MEMBER_ACTIVITY_SCORE
                );

        MultiplierPolicy momPolicy = policies.stream()
                .filter(p -> "MEMBER_OF_MONTH".equalsIgnoreCase(p.getRuleName()))
                .findFirst()
                .orElse(null);

        if (momPolicy == null) return;

        best.setActivityLevel(MemberActivityLevelEnum.MEMBER_OF_MONTH);
        best.setAppliedMultiplier(momPolicy.getMultiplier());
        best.setFinalScore(best.getBaseScore() * momPolicy.getMultiplier());

        // update multiplier của Membership
        Membership membership = best.getMembership();
        membership.setMemberMultiplier(momPolicy.getMultiplier());
        membershipRepo.save(membership);

        monthlyActivityRepo.save(best);
    }
}
