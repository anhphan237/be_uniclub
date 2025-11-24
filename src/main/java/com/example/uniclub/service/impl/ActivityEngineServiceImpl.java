package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.PerformanceDetailResponse;
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
    // 1) RECALCULATE FOR ONE MEMBER
    // =========================================================================
    @Override
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month) {

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        MemberMonthlyActivity activity = calculateMonthlyRecord(membership, year, month);

        log.info("Recalculated monthly activity for membership={}, {}/{}",
                membershipId, month, year);

        return activity;
    }

    // =========================================================================
    // 2) CORE CALCULATION (FINAL)
    // =========================================================================
    @Transactional
    protected MemberMonthlyActivity calculateMonthlyRecord(
            Membership membership,
            int year,
            int month
    ) {
        Long membershipId = membership.getMembershipId();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        // ============================
        // ATTENDANCE
        // ============================
        int totalSessions = attendanceRepo
                .countByMembership_MembershipIdAndSession_DateBetween(membershipId, start, end);

        int presentSessions = attendanceRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end
                );

        double sessionRate = totalSessions == 0 ? 0.0 : (double) presentSessions / totalSessions;

        int attendanceBase = resolveAttendanceBaseScore();
        double attendanceMul = resolveAttendanceMultiplier(sessionRate);
        int attendanceTotal = (int) Math.round(attendanceBase * attendanceMul);

        // ============================
        // STAFF PERFORMANCE
        // ============================
        List<StaffPerformance> staffList =
                staffPerformanceRepo.findByMembership_MembershipIdAndEvent_DateBetween(
                        membershipId, start, end
                );

        int totalStaffCount = staffList.size();

        PerformanceLevelEnum bestEval = resolveBestStaffEvaluation(staffList);

        double staffMultiplier = resolveStaffMultiplier(bestEval.name());
        int staffBaseScore = resolveStaffBaseScore();
        int staffScore = (int) Math.round(staffBaseScore * staffMultiplier);

        int penaltyPoints = 0; // có hệ thống penalty riêng thì kết nối lại

        int staffTotal = staffScore - penaltyPoints;

        // ============================
        // FINAL SCORE
        // ============================
        int finalScore = attendanceTotal + staffTotal;

        // ============================
        // SAVE
        // ============================
        MemberMonthlyActivity m = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElseGet(() -> MemberMonthlyActivity.builder()
                        .membership(membership)
                        .year(year)
                        .month(month)
                        .build()
                );

        // Event bỏ hoàn toàn
        m.setTotalEventRegistered(0);
        m.setTotalEventAttended(0);
        m.setEventAttendanceRate(0);

        // penalty
        m.setTotalPenaltyPoints(penaltyPoints);

        // attendance
        m.setTotalClubSessions(totalSessions);
        m.setTotalClubPresent(presentSessions);
        m.setSessionAttendanceRate(sessionRate);

        m.setAttendanceBaseScore(attendanceBase);
        m.setAttendanceMultiplier(attendanceMul);
        m.setAttendanceTotalScore(attendanceTotal);

        // staff
        m.setStaffBaseScore(staffBaseScore);
        m.setTotalStaffCount(totalStaffCount);
        m.setStaffEvaluation(bestEval.name());
        m.setStaffMultiplier(staffMultiplier);
        m.setStaffScore(staffScore);
        m.setStaffTotalScore(staffTotal);

        // activity level
        m.setActivityLevel(classifyActivityLevel(sessionRate).name());

        m.setFinalScore(finalScore);

        return monthlyRepo.save(m);
    }

    // =========================================================================
    // STAFF – CHỌN MỨC CAO NHẤT
    // =========================================================================
    private PerformanceLevelEnum resolveBestStaffEvaluation(List<StaffPerformance> list) {
        PerformanceLevelEnum best = PerformanceLevelEnum.POOR;
        for (StaffPerformance sp : list) {
            PerformanceLevelEnum level = PerformanceLevelEnum.valueOf(sp.getPerformance().name());
            if (level.ordinal() > best.ordinal()) {
                best = level;
            }
        }
        return best;
    }

    // =========================================================================
    // ACTIVITY LEVEL – DUY TRÊN SESSION RATE
    // =========================================================================
    private MemberActivityLevelEnum classifyActivityLevel(double sessionRate) {

        int percent = (int) Math.round(sessionRate * 100);

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
                return mapRuleToLevel(p.getRuleName());
            }
        }

        return MemberActivityLevelEnum.LOW; // fallback
    }

    private MemberActivityLevelEnum mapRuleToLevel(String rule) {
        return switch (rule) {
            case "LEVEL_FULL" -> MemberActivityLevelEnum.FULL;
            case "LEVEL_POSITIVE" -> MemberActivityLevelEnum.POSITIVE;
            case "LEVEL_NORMAL" -> MemberActivityLevelEnum.NORMAL;
            case "LEVEL_LOW" -> MemberActivityLevelEnum.LOW;
            default -> MemberActivityLevelEnum.LOW;
        };
    }



    // =========================================================================
    // POLICY RESOLUTION (KHÔNG HARDCODE)
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
                        HttpStatus.INTERNAL_SERVER_ERROR, "Missing Attendance BASE score"
                ));
    }

    private double resolveAttendanceMultiplier(double rate) {
        int percent = (int) Math.round(rate * 100);

        List<MultiplierPolicy> list =
                policyRepo.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.SESSION_ATTENDANCE
                );

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
                        HttpStatus.INTERNAL_SERVER_ERROR, "Missing Staff BASE score"
                ));
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
                        "Missing multiplier for staff rule=" + ruleName
                ));
    }

    // =========================================================================
    // 3) RECALCULATE ENTIRE CLUB
    // =========================================================================
    @Override
    @Transactional
    public void recalculateForClub(Long clubId, int year, int month) {
        List<Membership> list = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
        );

        for (Membership m : list) {
            calculateMonthlyRecord(m, year, month);
        }

        log.info("Recalculated monthly activity for club={}, {}/{}",
                clubId, month, year);
    }

    // =========================================================================
    // 4) RECALCULATE ALL CLUBS
    // =========================================================================
    @Override
    @Transactional
    public void recalculateAllForMonth(int year, int month) {
        List<Club> clubs = membershipRepo.findAll().stream()
                .map(Membership::getClub)
                .distinct()
                .toList();

        for (Club c : clubs) {
            recalculateForClub(c.getClubId(), year, month);
        }

        log.info("Recalculated monthly activity for ALL clubs, {}/{}", month, year);
    }

    // =========================================================================
    // 5) MEMBER SCORE (FINAL SCORE)
    // =========================================================================
    @Override
    public double calculateMemberScore(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Active membership not found")
                );

        LocalDate now = LocalDate.now();

        MemberMonthlyActivity act = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, now.getYear(), now.getMonthValue())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Monthly activity not found")
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
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Active membership not found")
                );

        LocalDate now = LocalDate.now();

        MemberMonthlyActivity act = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, now.getYear(), now.getMonthValue())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Monthly activity not found")
                );

        return PerformanceDetailResponse.builder()
                .baseScore(act.getAttendanceTotalScore() + act.getStaffTotalScore())
                .multiplier(1.0)
                .finalScore(act.getFinalScore())
                .build();
    }

    // =========================================================================
    // 7) GET MONTHLY ACTIVITY
    // =========================================================================
    @Override
    public MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Active membership not found")
                );

        return monthlyRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Monthly activity not found")
                );
    }

    // =========================================================================
    // 8) GET CLUB MONTHLY ACTIVITIES
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month) {
        return monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);
    }

    // =========================================================================
    // 9) GET CLUB RANKING
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month) {

        List<MemberMonthlyActivity> list =
                monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        list.sort(Comparator.comparingInt(MemberMonthlyActivity::getFinalScore).reversed());
        return list;
    }
}
