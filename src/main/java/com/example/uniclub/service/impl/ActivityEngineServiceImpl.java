package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.PerformanceDetailResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ActivityEngineService;
import com.example.uniclub.service.MultiplierPolicyService;
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
    private final EventRepository eventRepo;
    private final MultiplierPolicyService multiplierPolicyService;

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

        for (Membership m : members) {
            calculateActivityForMembership(m, year, month);
        }

        // 🔹 Sau khi tính từng member, chọn MEMBER_OF_MONTH cho CLB
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

        // 🔥 Sau khi chạy cho tất cả member → cập nhật multiplier cho CLB
        recalculateClubMultipliersForMonth(year, month);

        log.info("Recalculated monthly activity for ALL clubs ({}/{})", month, year);
    }

    // ==========================================================
    // 🔹 CORE CALCULATION CHO MEMBER
    // ==========================================================
    private static class PenaltyStats {
        int totalPoints;
        int violationCount;
        boolean hasRepeat;
    }

    @Transactional
    protected MemberMonthlyActivity calculateActivityForMembership(
            Membership membership,
            int year,
            int month
    ) {
        Long userId = membership.getUser().getUserId();
        Long membershipId = membership.getMembershipId();
        Long clubId = membership.getClub().getClubId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDateExclusive = startDate.plusMonths(1);

        // 1️⃣ Event participation
        var eventStats = computeEventStats(membership, startDate, endDateExclusive);

        // 2️⃣ Club session attendance (FIXED to membershipId + date)
        var sessionStats = computeClubSessionStats(membership, startDate, endDateExclusive.minusDays(1));

        // 3️⃣ Staff performance
        double avgStaffPerf = computeStaffPerformanceScore(
                membershipId,
                startDate,
                endDateExclusive.minusDays(1)
        );

        // 4️⃣ Penalty
        PenaltyStats penaltyStats = computePenaltyStats(
                membershipId,
                startDate.atStartOfDay(),
                endDateExclusive.atStartOfDay()
        );

        // 5️⃣ Base score
        double baseScore = computeBaseScore(
                eventStats,
                sessionStats,
                avgStaffPerf,
                penaltyStats.totalPoints
        );
        int basePercent = (int) Math.round(baseScore * 100);

        // 6️⃣ Multiplier from policy
        var multiplierResult = resolveMultiplierFromPolicy(basePercent);
        MemberActivityLevelEnum level = multiplierResult.level();
        double appliedMultiplier = multiplierResult.multiplier();

        double finalScore = baseScore * appliedMultiplier;

        // 🔻 Repeat violation penalty
        if (penaltyStats.hasRepeat) {
            finalScore = finalScore * 0.8;
        }

        // 7️⃣ Save or update MonthlyActivity
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
        monthly.setTotalPenaltyPoints(penaltyStats.totalPoints);
        monthly.setBaseScore(baseScore);
        monthly.setBaseScorePercent(basePercent);
        monthly.setActivityLevel(level);
        monthly.setAppliedMultiplier(appliedMultiplier);
        monthly.setFinalScore(finalScore);

        // Update membership multiplier
        membership.setMemberMultiplier(appliedMultiplier);
        membershipRepo.save(membership);

        return monthlyActivityRepo.save(monthly);
    }


    // ==========================================================
    // 🔹 EVENT STATS (tham gia event trong tháng theo CLB)
    // ==========================================================
    private static class EventStats {
        int totalRegistered;         // số event member đã đăng ký (trong CLB)
        int totalAttendedWeighted;   // số event đã tham gia (FULL = 1, HALF = 0.5 ~ làm tròn)
        double attendanceRatio;      // 0–1 = (weighted_attended / tổng số event CLB tổ chức)
    }

    private EventStats computeEventStats(
            Membership membership,
            LocalDate start,
            LocalDate endExclusive
    ) {
        Long userId = membership.getUser().getUserId();
        Long clubId = membership.getClub().getClubId();

        List<EventRegistration> all = eventRegistrationRepo
                .findByUser_UserIdOrderByRegisteredAtDesc(userId);

        List<EventRegistration> inMonthAndClub = all.stream()
                .filter(r -> r.getEvent() != null && r.getEvent().getDate() != null)
                .filter(r -> r.getEvent().getHostClub() != null &&
                        Objects.equals(r.getEvent().getHostClub().getClubId(), clubId))
                .filter(r -> {
                    LocalDate d = r.getEvent().getDate();
                    return !d.isBefore(start) && d.isBefore(endExclusive);
                })
                .toList();

        // Count club events completed this month
        List<Event> clubEventsInMonth = eventRepo.findCompletedEventsOfClubInRange(
                clubId,
                start,
                endExclusive.minusDays(1)
        );

        int totalClubEvents = clubEventsInMonth.size();
        int totalRegistered = inMonthAndClub.size();

        double attendedWeighted = 0;
        for (EventRegistration r : inMonthAndClub) {
            if (r.getAttendanceLevel() == AttendanceLevelEnum.FULL) attendedWeighted += 1.0;
            else if (r.getAttendanceLevel() == AttendanceLevelEnum.HALF) attendedWeighted += 0.5;
        }

        EventStats stats = new EventStats();
        stats.totalRegistered = totalRegistered;
        stats.totalAttendedWeighted = (int) Math.round(attendedWeighted);
        stats.attendanceRatio = totalClubEvents == 0 ? 0.0 :
                clamp01(attendedWeighted / totalClubEvents);

        return stats;
    }


    // ==========================================================
    // 🔹 CLUB SESSION STATS (điểm danh buổi sinh hoạt)
    // ==========================================================
    private static class SessionStats {
        int totalSessions;
        int presentSessions;
        double attendanceRatio;
    }

    private SessionStats computeClubSessionStats(
            Membership membership,
            LocalDate start,
            LocalDate endInclusive
    ) {
        Long membershipId = membership.getMembershipId();

        int totalSessions = clubAttendanceRecordRepo
                .countByMembership_MembershipIdAndSession_DateBetween(
                        membershipId,
                        start,
                        endInclusive
                );

        int present = clubAttendanceRecordRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start,
                        endInclusive
                );

        SessionStats stats = new SessionStats();
        stats.totalSessions = totalSessions;
        stats.presentSessions = present;
        stats.attendanceRatio =
                totalSessions == 0 ? 0.0 : clamp01((double) present / totalSessions);

        return stats;
    }


    // ==========================================================
    // 🔹 STAFF PERFORMANCE (0–1) theo bảng yêu cầu
    // ==========================================================
    private double computeStaffPerformanceScore(
            Long membershipId,
            LocalDate start,
            LocalDate endInclusive
    ) {
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

        return clamp01(sum / list.size());
    }


    /**
     * Bám bảng:
     * POOR      -> 0
     * AVERAGE   -> 0.4
     * GOOD      -> 0.8
     * EXCELLENT -> 1.0
     */
    private double mapPerformanceLevelToScore(PerformanceLevelEnum level) {
        if (level == null) return 0.0;
        return switch (level) {
            case POOR -> 0.0;
            case AVERAGE -> 0.4;
            case GOOD -> 0.8;
            case EXCELLENT -> 1.0;
        };
    }

    // ==========================================================
    // 🔹 PENALTY POINTS + REPEAT VIOLATION
    // ==========================================================
    private PenaltyStats computePenaltyStats(
            Long membershipId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        List<ClubPenalty> penalties = clubPenaltyRepo
                .findByMembership_MembershipIdAndCreatedAtBetween(
                        membershipId,
                        start,
                        end
                );

        PenaltyStats stats = new PenaltyStats();
        stats.totalPoints = penalties.stream()
                .mapToInt(ClubPenalty::getPoints)
                .sum();
        stats.violationCount = penalties.size();
        stats.hasRepeat = penalties.size() >= 3;

        return stats;
    }


    /**
     * Map tổng điểm phạt sang factor 0–1.
     * Dựa gần giống bảng vi phạm:
     *  - <= -50  -> 0.0
     *  - -30..-49 -> 0.25
     *  - -15..-29 -> 0.5
     *  - -1..-14  -> 0.75
     *  - >= 0     -> 1.0
     */
    private double mapPenaltyToFactor(int totalPenaltyPoints) {
        if (totalPenaltyPoints >= 0) return 1.0;       // không bị phạt
        if (totalPenaltyPoints <= -50) return 0.0;
        if (totalPenaltyPoints <= -30) return 0.25;
        if (totalPenaltyPoints <= -15) return 0.5;
        return 0.75; // -15 < points < 0
    }

    // ==========================================================
    // 🔹 BASE SCORE (0–1) – CÓ TRỌNG SỐ
    // ==========================================================
    private double computeBaseScore(
            EventStats eventStats,
            SessionStats sessionStats,
            double staffScore,
            int totalPenaltyPoints
    ) {
        double eventScore = eventStats.attendanceRatio;
        double sessionScore = sessionStats.attendanceRatio;
        double penaltyFactor = mapPenaltyToFactor(totalPenaltyPoints);

        return clamp01(
                eventScore * 0.56 +
                        sessionScore * 0.16 +
                        staffScore * 0.23 +
                        penaltyFactor * 0.05
        );
    }


    // ==========================================================
    // 🔹 RESOLVE MULTIPLIER FROM POLICY (MEMBER_ACTIVITY_SCORE)
    // ==========================================================
    private record MultiplierResult(MemberActivityLevelEnum level, double multiplier) {}

    private MultiplierResult resolveMultiplierFromPolicy(int baseScorePercent) {

        List<MultiplierPolicy> policies = multiplierPolicyRepo
                .findByTargetTypeAndActivityTypeAndActiveIsTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.MEMBER_ACTIVITY_SCORE
                );

        for (MultiplierPolicy p : policies) {
            Integer min = p.getMinThreshold();
            Integer max = p.getMaxThreshold();

            boolean okMin = baseScorePercent >= min;
            boolean okMax = (max == null) || baseScorePercent <= max;

            if (okMin && okMax) {
                MemberActivityLevelEnum lvl;
                try {
                    lvl = MemberActivityLevelEnum.valueOf(p.getRuleName());
                } catch (Exception e) {
                    lvl = MemberActivityLevelEnum.NORMAL;
                }
                return new MultiplierResult(lvl, p.getMultiplier());
            }
        }

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

        List<MemberMonthlyActivity> fullList = list.stream()
                .filter(m -> m.getActivityLevel() == MemberActivityLevelEnum.FULL)
                .toList();

        if (fullList.isEmpty()) return;

        MemberMonthlyActivity best = fullList.stream()
                .max(Comparator.comparingDouble(MemberMonthlyActivity::getFinalScore))
                .orElse(null);

        if (best == null) return;

        MultiplierPolicy mom = multiplierPolicyRepo
                .findByTargetTypeAndActivityTypeAndActiveIsTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.MEMBER_ACTIVITY_SCORE
                ).stream()
                .filter(p -> "MEMBER_OF_MONTH".equalsIgnoreCase(p.getRuleName()))
                .findFirst()
                .orElse(null);

        if (mom == null) return;

        best.setActivityLevel(MemberActivityLevelEnum.MEMBER_OF_MONTH);
        best.setAppliedMultiplier(mom.getMultiplier());
        best.setFinalScore(best.getBaseScore() * mom.getMultiplier());

        Membership m = best.getMembership();
        m.setMemberMultiplier(mom.getMultiplier());
        membershipRepo.save(m);

        monthlyActivityRepo.save(best);
    }


    // ==========================================================
    // 🔹 CLUB MULTIPLIER (Đánh giá hoạt động CLB theo số event)
    // ==========================================================
    @Transactional
    protected void recalculateClubMultipliersForMonth(int year, int month) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate endExclusive = start.plusMonths(1);

        List<Club> clubs = clubRepo.findAll();
        if (clubs.isEmpty()) return;

        int bestEventCount = 0;
        List<Club> bestClubs = new ArrayList<>();

        List<MultiplierPolicy> policies = multiplierPolicyRepo
                .findByTargetTypeAndActivityTypeAndActiveIsTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.CLUB,
                        PolicyActivityTypeEnum.CLUB_EVENT_ORGANIZATION
                );

        for (Club club : clubs) {
            int eventCount = eventRepo.countByHostClub_ClubIdAndStatusAndDateBetween(
                    club.getClubId(),
                    EventStatusEnum.COMPLETED,
                    start,
                    endExclusive.minusDays(1)
            );

            double mul = resolveClubMultiplierFromPolicies(policies, eventCount);
            club.setClubMultiplier(mul);
            clubRepo.save(club);

            if (eventCount > bestEventCount) {
                bestEventCount = eventCount;
                bestClubs = new ArrayList<>(List.of(club));
            } else if (eventCount == bestEventCount && eventCount > 0) {
                bestClubs.add(club);
            }
        }

        MultiplierPolicy clubOfMonth = policies.stream()
                .filter(p -> "CLUB_OF_MONTH".equalsIgnoreCase(p.getRuleName()))
                .findFirst()
                .orElse(null);

        if (bestEventCount > 0 && clubOfMonth != null) {
            for (Club club : bestClubs) {
                club.setClubMultiplier(clubOfMonth.getMultiplier());
                clubRepo.save(club);
            }
        }
    }



    private double resolveClubMultiplierFromPolicies(
            List<MultiplierPolicy> policies,
            int eventCount
    ) {
        for (MultiplierPolicy p : policies) {
            if ("CLUB_OF_MONTH".equalsIgnoreCase(p.getRuleName())) continue;

            Integer min = p.getMinThreshold();
            Integer max = p.getMaxThreshold();

            boolean okMin = eventCount >= min;
            boolean okMax = (max == null) || eventCount <= max;

            if (okMin && okMax) {
                return p.getMultiplier();
            }
        }
        return 1.0;
    }
    private double clamp01(double v) {
        if (v < 0) return 0.0;
        if (v > 1) return 1.0;
        return v;
    }
    // ==========================================================
// 🔥 REPLACE REALTIME CALCULATION WITH MONTHLY RESULT
// ==========================================================

    @Override
    public double calculateMemberScore(Long memberId) {

        // 1) lấy membership
        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active membership not found"));

        // 2) xác định thời gian tháng hiện tại
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // 3) lấy monthly activity đã tính
        MemberMonthlyActivity monthly = monthlyActivityRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElse(null);

        // 4) nếu chưa có → tự tính monthly 1 lần → rồi trả về
        if (monthly == null) {
            monthly = calculateActivityForMembership(membership, year, month);
        }

        return clamp01(monthly.getFinalScore());
    }



    // ==========================================================
// 🔥 CHI TIẾT PERFORMANCE (BASE / MULTIPLIER / FINAL)
// ==========================================================
    @Override
    public PerformanceDetailResponse calculateMemberScoreDetail(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active membership not found"));


        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        MemberMonthlyActivity monthly = monthlyActivityRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElse(null);

        if (monthly == null) {
            monthly = calculateActivityForMembership(membership, year, month);
        }

        return PerformanceDetailResponse.builder()
                .baseScore(monthly.getBaseScore())
                .multiplier(monthly.getAppliedMultiplier())
                .finalScore(monthly.getFinalScore())
                .build();
    }
    @Override
    public MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active membership not found"));

        return monthlyActivityRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No monthly activity found for this member in the specified month"));
    }
    @Override
    public List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month) {

        return monthlyActivityRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);
    }
    @Override
    public List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month) {

        List<MemberMonthlyActivity> list =
                monthlyActivityRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        list.sort(Comparator.comparingDouble(MemberMonthlyActivity::getFinalScore).reversed());

        return list;
    }

}
