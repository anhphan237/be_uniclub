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

    // ========================================================================
    // 1) RECALCULATE FOR ONE MEMBER
    // ========================================================================
    @Override
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month) {

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        MemberMonthlyActivity activity = calculateMonthlyRecord(membership, year, month);

        log.info("Recalculated monthly activity for membership={} / {}/{}",
                membershipId, month, year);

        return activity;
    }

    // ========================================================================
    // 2) CORE CALCULATION (THEO EXCEL MODEL)
    // ========================================================================
    @Transactional
    protected MemberMonthlyActivity calculateMonthlyRecord(
            Membership membership,
            int year,
            int month
    ) {
        Long membershipId = membership.getMembershipId();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        // ============ ATTENDANCE STATS ============
        int totalSessions = attendanceRepo
                .countByMembership_MembershipIdAndSession_DateBetween(membershipId, start, end);

        int presentSessions = attendanceRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end
                );

        double rate = totalSessions == 0 ? 0.0 : (double) presentSessions / totalSessions;

        int attendanceBase = resolveAttendanceBaseScore();
        double attendanceMul = resolveAttendanceMultiplier(rate);
        int attendanceTotal = (int) Math.round(attendanceBase * attendanceMul);

        // ============ STAFF PERFORMANCE ============
        List<StaffPerformance> staffList =
                staffPerformanceRepo.findByMembership_MembershipIdAndEvent_DateBetween(
                        membershipId, start, end
                );

        int good = 0, avg = 0, poor = 0;
        for (StaffPerformance sp : staffList) {
            switch (sp.getPerformance()) {
                case GOOD -> good++;
                case AVERAGE -> avg++;
                case POOR -> poor++;
            }
        }

        int staffBase = resolveStaffBaseScore();
        double mulGood = resolveStaffMultiplier("GOOD");
        double mulAvg  = resolveStaffMultiplier("AVERAGE");
        double mulPoor = resolveStaffMultiplier("POOR");

        int scoreGood = (int) Math.round(staffBase * mulGood * good);
        int scoreAvg  = (int) Math.round(staffBase * mulAvg  * avg);
        int scorePoor = (int) Math.round(staffBase * mulPoor * poor);

        int staffTotal = scoreGood + scoreAvg + scorePoor;

        // ============ FINAL SCORE ============
        int finalScore = attendanceTotal + staffTotal;

        // ============ SAVE ============
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

        m.setTotalClubSessions(totalSessions);
        m.setTotalClubPresent(presentSessions);

        m.setStaffGoodCount(good);
        m.setStaffAverageCount(avg);
        m.setStaffPoorCount(poor);

        m.setAttendanceBaseScore(attendanceBase);
        m.setAttendanceMultiplier(attendanceMul);
        m.setAttendanceTotalScore(attendanceTotal);

        m.setStaffBaseScore(staffBase);
        m.setStaffScoreGood(scoreGood);
        m.setStaffScoreAverage(scoreAvg);
        m.setStaffScorePoor(scorePoor);
        m.setStaffTotalScore(staffTotal);

        m.setFinalScore(finalScore);

        return monthlyRepo.save(m);
    }

    // ========================================================================
    // 3) RECALCULATE FOR ENTIRE CLUB
    // ========================================================================
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

        log.info("Recalculated monthly activity for entire club={} / {}/{}",
                clubId, month, year);
    }

    // ========================================================================
    // 4) RECALCULATE FOR ALL CLUBS
    // ========================================================================
    @Override
    @Transactional
    public void recalculateAllForMonth(int year, int month) {

        List<Club> allClubs = membershipRepo.findAll().stream()
                .map(Membership::getClub)
                .distinct()
                .toList();

        for (Club club : allClubs) {
            recalculateForClub(club.getClubId(), year, month);
        }

        log.info("Recalculated monthly activity for ALL clubs / {}/{}", month, year);
    }

    // ========================================================================
    // 5) MEMBER SCORE (Excel Model → return finalScore)
    // ========================================================================
    @Override
    public double calculateMemberScore(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        LocalDate now = LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();

        MemberMonthlyActivity act = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, y, m)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No monthly activity found for this member"));

        return act.getFinalScore();
    }

    // ========================================================================
    // 6) SCORE DETAIL (attendance + staff + final)
    // ========================================================================
    @Override
    public PerformanceDetailResponse calculateMemberScoreDetail(Long memberId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        LocalDate now = LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();

        MemberMonthlyActivity act = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, y, m)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Monthly activity not found"));

        return PerformanceDetailResponse.builder()
                .baseScore(act.getAttendanceTotalScore() + act.getStaffTotalScore())
                .multiplier(1.0) // Excel model không dùng multiplier nữa
                .finalScore(act.getFinalScore())
                .build();
    }

    // ========================================================================
    // 7) GET MONTHLY ACTIVITY
    // ========================================================================
    @Override
    public MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(memberId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        return monthlyRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Monthly activity not found"));
    }

    // ========================================================================
    // 8) GET CLUB MONTHLY ACTIVITIES
    // ========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month) {
        return monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);
    }

    // ========================================================================
    // 9) GET CLUB RANKING
    // ========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month) {

        List<MemberMonthlyActivity> list =
                monthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        list.sort(Comparator.comparingInt(MemberMonthlyActivity::getFinalScore).reversed());

        return list;
    }

    // ========================================================================
    // RULE RESOLUTION — ATTENDANCE
    // ========================================================================
    private int resolveAttendanceBaseScore() {
        return policyRepo
                .findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.SESSION_ATTENDANCE,
                        "BASE"
                )
                .map(MultiplierPolicy::getMinThreshold)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Missing Attendance BASE score"
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

    // ========================================================================
    // RULE RESOLUTION — STAFF
    // ========================================================================
    private int resolveStaffBaseScore() {
        return policyRepo
                .findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.STAFF_EVALUATION,
                        "BASE"
                )
                .map(MultiplierPolicy::getMinThreshold)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Missing Staff BASE score"
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
                        "Missing staff multiplier for rule=" + ruleName
                ));
    }
}
