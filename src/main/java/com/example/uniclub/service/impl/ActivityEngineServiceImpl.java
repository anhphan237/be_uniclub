package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ActivityEngineService;
import com.example.uniclub.dto.response.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEngineServiceImpl implements ActivityEngineService {

    private final MembershipRepository membershipRepo;
    private final ClubAttendanceRecordRepository attendanceRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;
    private final MemberMonthlyActivityRepository monthlyRepo;
    private final MultiplierPolicyRepository policyRepo;

    // =========================================================================
    //  🔒 LOCK CHECK
    // =========================================================================
    private void ensureMonthEditable(int year, int month) {
        validateMonth(year, month);

        LocalDate now = LocalDate.now();
        LocalDate end = LocalDate.of(year, month, 1)
                .plusMonths(1)
                .minusDays(1);

        if (now.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Monthly activity for %d/%d is locked.".formatted(month, year));
        }
    }

    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must be between 1–12.");
        }
        if (year < 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year.");
        }
    }

    // =========================================================================
    //  AUTO-CREATE MONTH IF MISSING (Only current month)
    // =========================================================================
    @Transactional
    public MemberMonthlyActivity autoCreateIfMissing(Membership membership, int year, int month) {
        return monthlyRepo.findByMembershipAndYearAndMonth(membership, year, month)
                .orElseGet(() -> {
                    // current month only
                    LocalDate now = LocalDate.now();
                    if (year == now.getYear() && month == now.getMonthValue()) {
                        return calculateMonthlyRecord(membership, year, month);
                    }
                    throw new ApiException(HttpStatus.NOT_FOUND,
                            "Monthly activity not found.");
                });
    }

    // =========================================================================
    // 1) RECALCULATE ONE MEMBER
    // =========================================================================
    @Override
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month) {

        validateMonth(year, month);
        ensureMonthEditable(year, month);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        return calculateMonthlyRecord(membership, year, month);
    }

    // =========================================================================
    // 2) CORE CALCULATION
    // =========================================================================
    @Transactional
    protected MemberMonthlyActivity calculateMonthlyRecord(Membership membership,
                                                           int year, int month) {

        Long membershipId = membership.getMembershipId();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.plusMonths(1).minusDays(1);

        // ================= ATTENDANCE =======================
        int totalSessions = attendanceRepo
                .countByMembership_MembershipIdAndSession_DateBetween(membershipId, start, end);

        int presentSessions = attendanceRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end
                );

        double rate = totalSessions == 0 ? 0 : (double) presentSessions / totalSessions;

        int attendanceBase = resolveAttendanceBaseScore();
        double attendanceMul = resolveAttendanceMultiplier(rate);
        int attendanceTotal = (int) Math.round(attendanceBase * attendanceMul);

        // ================= STAFF PERFORMANCE ================
        List<StaffPerformance> staffList =
                staffPerformanceRepo.findByMembership_MembershipIdAndEvent_DateBetween(
                        membershipId, start, end);

        PerformanceLevelEnum bestEval = resolveBestStaffEvaluation(staffList);

        double staffMult = resolveStaffMultiplier(bestEval.name());
        int staffBaseScore = resolveStaffBaseScore();
        int staffScore = (int) Math.round(staffBaseScore * staffMult);

        // no penalty system (reserved)
        int finalScore = attendanceTotal + staffScore;

        // ================= SAVE =============================
        MemberMonthlyActivity m = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElse(MemberMonthlyActivity.builder()
                        .membership(membership)
                        .year(year)
                        .month(month)
                        .build()
                );

        m.setTotalClubSessions(totalSessions);
        m.setTotalClubPresent(presentSessions);
        m.setSessionAttendanceRate(rate);

        m.setAttendanceBaseScore(attendanceBase);
        m.setAttendanceMultiplier(attendanceMul);
        m.setAttendanceTotalScore(attendanceTotal);

        m.setStaffBaseScore(staffBaseScore);
        m.setStaffMultiplier(staffMult);
        m.setStaffScore(staffScore);
        m.setTotalStaffCount(staffList.size());

        m.setStaffTotalScore(staffScore);
        m.setActivityLevel(classifyActivityLevel(rate).name());
        m.setFinalScore(finalScore);

        // event removed
        m.setTotalEventRegistered(0);
        m.setTotalEventAttended(0);
        m.setEventAttendanceRate(0);

        m.setTotalPenaltyPoints(0);

        return monthlyRepo.save(m);
    }

    // =========================================================================
    // STAFF BEST EVALUATION
    // =========================================================================
    private PerformanceLevelEnum resolveBestStaffEvaluation(List<StaffPerformance> list) {
        PerformanceLevelEnum best = PerformanceLevelEnum.POOR;
        for (StaffPerformance s : list) {
            PerformanceLevelEnum lvl = PerformanceLevelEnum.valueOf(s.getPerformance().name());
            if (lvl.ordinal() > best.ordinal()) best = lvl;
        }
        return best;
    }

    // =========================================================================
    // ACTIVITY LEVEL
    // =========================================================================
    private MemberActivityLevelEnum classifyActivityLevel(double rate) {
        int percent = (int) Math.round(rate * 100);

        List<MultiplierPolicy> rules = policyRepo
                .findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.SESSION_ATTENDANCE
                );

        for (MultiplierPolicy p : rules) {
            if (!p.getRuleName().startsWith("LEVEL_")) continue;

            boolean okMin = percent >= p.getMinThreshold();
            boolean okMax = p.getMaxThreshold() == null || percent <= p.getMaxThreshold();

            if (okMin && okMax) {
                return switch (p.getRuleName()) {
                    case "LEVEL_FULL" -> MemberActivityLevelEnum.FULL;
                    case "LEVEL_POSITIVE" -> MemberActivityLevelEnum.POSITIVE;
                    case "LEVEL_NORMAL" -> MemberActivityLevelEnum.NORMAL;
                    case "LEVEL_LOW" -> MemberActivityLevelEnum.LOW;
                    default -> MemberActivityLevelEnum.LOW;
                };
            }
        }

        return MemberActivityLevelEnum.LOW;
    }

    // =========================================================================
    // POLICY RESOLVER
    // =========================================================================
    private int resolveAttendanceBaseScore() {
        return policyRepo
                .findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.SESSION_ATTENDANCE,
                        "BASE"
                )
                .map(MultiplierPolicy::getMinThreshold)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Missing Attendance BASE score"));
    }

    private double resolveAttendanceMultiplier(double rate) {
        int percent = (int) (rate * 100);

        List<MultiplierPolicy> list = policyRepo
                .findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.SESSION_ATTENDANCE);

        for (MultiplierPolicy p : list) {
            if ("BASE".equals(p.getRuleName())) continue;

            boolean okMin = percent >= p.getMinThreshold();
            boolean okMax = p.getMaxThreshold() == null || percent <= p.getMaxThreshold();

            if (okMin && okMax) return p.getMultiplier();
        }

        return 1.0;
    }

    private int resolveStaffBaseScore() {
        return policyRepo
                .findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.STAFF_EVALUATION,
                        "BASE"
                )
                .map(MultiplierPolicy::getMinThreshold)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Missing Staff BASE score"));
    }

    private double resolveStaffMultiplier(String ruleName) {
        return policyRepo
                .findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.STAFF_EVALUATION,
                        ruleName
                )
                .map(MultiplierPolicy::getMultiplier)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Missing staff rule=" + ruleName));
    }

    // =========================================================================
    // 3) RECALCULATE ENTIRE CLUB
    // =========================================================================
    @Override
    @Transactional
    public void recalculateForClub(Long clubId, int year, int month) {

        validateMonth(year, month);
        ensureMonthEditable(year, month);

        List<Membership> list = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED));

        for (Membership m : list) {
            calculateMonthlyRecord(m, year, month);
        }
    }

    // =========================================================================
    // 4) RECALCULATE ALL CLUBS
    // =========================================================================
    @Override
    @Transactional
    public void recalculateAllForMonth(int year, int month) {

        validateMonth(year, month);
        ensureMonthEditable(year, month);

        List<Club> clubs = membershipRepo.findAll().stream()
                .map(Membership::getClub)
                .distinct()
                .toList();

        for (Club c : clubs) {
            recalculateForClub(c.getClubId(), year, month);
        }
    }

    // =========================================================================
    // 5) GET SCORE
    // =========================================================================
    @Override
    public double calculateMemberScore(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        LocalDate now = LocalDate.now();

        MemberMonthlyActivity act = autoCreateIfMissing(
                membership,
                now.getYear(),
                now.getMonthValue()
        );

        return act.getFinalScore();
    }

    // =========================================================================
    // 6) SCORE DETAIL
    // =========================================================================
    @Override
    public PerformanceDetailResponse calculateMemberScoreDetail(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        LocalDate now = LocalDate.now();

        MemberMonthlyActivity act = autoCreateIfMissing(
                membership,
                now.getYear(),
                now.getMonthValue()
        );

        return PerformanceDetailResponse.builder()
                .baseScore(act.getAttendanceTotalScore() + act.getStaffTotalScore())
                .multiplier(1.0)
                .finalScore(act.getFinalScore())
                .build();
    }

    // =========================================================================
    // 7) GET MONTHLY ACTIVITY (AUTO GENERATE CURRENT MONTH)
    // =========================================================================
    @Override
    public MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month) {

        validateMonth(year, month);

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        return autoCreateIfMissing(membership, year, month);
    }

    // =========================================================================
    // 8) GET CLUB LIST
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month) {
        validateMonth(year, month);
        return monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);
    }

    // =========================================================================
    // 9) CLUB RANKING
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month) {
        validateMonth(year, month);
        return monthlyRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month)
                .stream()
                .sorted(Comparator.comparingInt(MemberMonthlyActivity::getFinalScore).reversed())
                .toList();
    }

    // =========================================================================
    // 10) PREVIEW SCORE
    // =========================================================================
    @Override
    public CalculateScoreResponse calculatePreviewScore(Long membershipId,
                                                        int attendanceBase,
                                                        int staffBase) {

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.plusMonths(1).minusDays(1);

        int totalSessions = attendanceRepo.countByMembership_MembershipIdAndSession_DateBetween(
                membershipId, start, end);

        int presentSessions = attendanceRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end);

        double rate = totalSessions == 0 ? 0 : (double) presentSessions / totalSessions;

        double attendanceMul = resolveAttendanceMultiplier(rate);
        int attendanceTotal  = (int) Math.round(attendanceBase * attendanceMul);

        List<StaffPerformance> staffList =
                staffPerformanceRepo.findByMembership_MembershipIdAndEvent_DateBetween(
                        membershipId, start, end);

        PerformanceLevelEnum best = resolveBestStaffEvaluation(staffList);

        double staffMul = resolveStaffMultiplier(best.name());
        int staffTotal  = (int) Math.round(staffBase * staffMul);

        return CalculateScoreResponse.builder()
                .attendanceBaseScore(attendanceBase)
                .attendanceMultiplier(attendanceMul)
                .attendanceTotalScore(attendanceTotal)
                .staffBaseScore(staffBase)
                .staffMultiplier(staffMul)
                .staffTotalScore(staffTotal)
                .finalScore(attendanceTotal + staffTotal)
                .build();
    }

    // =========================================================================
    // 11) LIVE ACTIVITY LIST
    // =========================================================================
    @Override
    public List<CalculateLiveActivityResponse> calculateLiveActivities(Long clubId,
                                                                       int attendanceBase,
                                                                       int staffBase) {

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        List<Membership> members = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED));

        return members.stream().map(m -> {
                    Long membershipId = m.getMembershipId();

                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end   = start.plusMonths(1).minusDays(1);

                    int totalSessions = attendanceRepo
                            .countByMembership_MembershipIdAndSession_DateBetween(membershipId, start, end);

                    int presentSessions = attendanceRepo
                            .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                                    membershipId,
                                    List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                                    start, end);

                    double rate = totalSessions == 0 ? 0 : (double) presentSessions / totalSessions;

                    double attMul = resolveAttendanceMultiplier(rate);
                    int attTotal  = (int) Math.round(attendanceBase * attMul);

                    List<StaffPerformance> staffList = staffPerformanceRepo
                            .findByMembership_MembershipIdAndEvent_DateBetween(
                                    membershipId, start, end);

                    PerformanceLevelEnum bestEval = resolveBestStaffEvaluation(staffList);

                    double staffMul = resolveStaffMultiplier(bestEval.name());
                    int staffTotal  = (int) Math.round(staffBase * staffMul);

                    int finalScore = attTotal + staffTotal;

                    return CalculateLiveActivityResponse.builder()
                            .membershipId(m.getMembershipId())
                            .userId(m.getUser().getUserId())
                            .fullName(m.getUser().getFullName())
                            .studentCode(m.getUser().getStudentCode())
                            .attendanceBaseScore(attendanceBase)
                            .attendanceMultiplier(attMul)
                            .attendanceTotalScore(attTotal)
                            .staffBaseScore(staffBase)
                            .staffMultiplier(staffMul)
                            .staffTotalScore(staffTotal)
                            .finalScore(finalScore)
                            .build();

                }).sorted((a, b) -> Integer.compare(b.getFinalScore(), a.getFinalScore()))
                .toList();
    }
}
