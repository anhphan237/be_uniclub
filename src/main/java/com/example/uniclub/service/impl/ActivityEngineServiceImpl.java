package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.CalculateScoreResponse;
import com.example.uniclub.dto.response.MemberMonthlyActivityResponse;
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
import java.util.Objects;

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
    // Validate Month
    // =========================================================================
    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must be between 1-12.");
        if (year < 2000)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year.");
    }

    // =========================================================================
    // Month Lock Check
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

    // =========================================================================
    // AUTO CREATE IF MISSING (Only current month)
    // =========================================================================
    @Transactional
    public MemberMonthlyActivity autoCreateIfMissing(Membership membership, int year, int month) {

        return monthlyRepo.findByMembershipAndYearAndMonth(membership, year, month)
                .orElseGet(() -> {
                    LocalDate now = LocalDate.now();
                    if (year == now.getYear() && month == now.getMonthValue()) {
                        return calculateMonthlyRecord(membership, year, month);
                    }
                    throw new ApiException(HttpStatus.NOT_FOUND, "Monthly activity not found.");
                });
    }

    // =========================================================================
    // Recalculate one member
    // =========================================================================
    @Override
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month) {

        validateMonth(year, month);
        ensureMonthEditable(year, month);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        return calculateMonthlyRecord(membership, year, month);
    }

    // =========================================================================
    // CORE CALCULATION
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

        // ================= ATTENDANCE =================
        int totalSessions = attendanceRepo
                .countByMembership_MembershipIdAndSession_DateBetween(
                        membershipId, start, end
                );

        int presentSessions = attendanceRepo
                .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end
                );

        double attendanceRate = totalSessions == 0
                ? 0
                : (double) presentSessions / totalSessions;

        // 🔑 BASE từ POLICY
        int attendanceBase = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.ATTENDANCE_BASE
        );

        double attendanceMultiplier = resolveMultiplier(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.SESSION_ATTENDANCE,
                (int) (attendanceRate * 100)
        );

        int attendanceTotal =
                (int) Math.round(attendanceBase * attendanceMultiplier);

        // ================= STAFF =================
        List<StaffPerformance> staffList =
                staffPerformanceRepo.findPerformanceInRange(
                        membershipId, start, end
                );

        PerformanceLevelEnum bestEvaluation =
                resolveBestStaffEvaluation(staffList);

        // 🔑 STAFF BASE từ POLICY
        int staffBase = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.STAFF_POINT
        );

        double staffMultiplier =
                resolveStaffMultiplier(bestEvaluation.name());

        int staffScore = staffBase; // điểm cho 1 lần
        int staffTotal =
                (int) Math.round(staffBase * staffMultiplier);

        // ================= FINAL SCORE =================
        int finalScore = attendanceTotal + staffTotal;

        // ================= SAVE / UPDATE =================
        MemberMonthlyActivity act = monthlyRepo
                .findByMembershipAndYearAndMonth(membership, year, month)
                .orElse(
                        MemberMonthlyActivity.builder()
                                .membership(membership)
                                .year(year)
                                .month(month)
                                .build()
                );

        // ===== CLUB / SESSION =====
        act.setTotalClubSessions(totalSessions);
        act.setTotalClubPresent(presentSessions);
        act.setSessionAttendanceRate(attendanceRate);

        // ===== ATTENDANCE =====
        act.setAttendanceBaseScore(attendanceBase);
        act.setAttendanceMultiplier(attendanceMultiplier);
        act.setAttendanceTotalScore(attendanceTotal);

        // ===== STAFF =====
        act.setStaffBaseScore(staffBase);
        act.setStaffMultiplier(staffMultiplier);
        act.setStaffScore(staffScore);
        act.setStaffTotalScore(staffTotal);
        act.setTotalStaffCount(staffList.size());
        act.setStaffEvaluation(bestEvaluation.name()); // ❗ NOT NULL

        // ===== EVENT (CHƯA TÍNH) =====
        act.setTotalEventRegistered(0);
        act.setTotalEventAttended(0);
        act.setEventAttendanceRate(0);
        act.setTotalPenaltyPoints(0);

        // ===== FINAL =====
        act.setActivityLevel(resolveLevelByFinalScore(finalScore));
        act.setFinalScore(finalScore);

        return monthlyRepo.save(act);
    }



    // =========================================================================
    // BEST STAFF LEVEL
    // =========================================================================
    private PerformanceLevelEnum resolveBestStaffEvaluation(List<StaffPerformance> list) {
        PerformanceLevelEnum best = PerformanceLevelEnum.POOR;
        for (StaffPerformance p : list) {
            PerformanceLevelEnum lvl = p.getPerformance();
            if (lvl.ordinal() > best.ordinal()) best = lvl;
        }
        return best;
    }

//    private String classifyLevelByFinalScore(int finalScore) {
//        if (finalScore >= 180) return "LEVEL_EXCELLENT";
//        if (finalScore >= 120) return "LEVEL_GOOD";
//        if (finalScore >= 80)  return "LEVEL_AVERAGE";
//        return "LEVEL_LOW";
//    }

    // =========================================================================
    // STAFF MULTIPLIER FROM POLICY
    // =========================================================================
    private double resolveStaffMultiplier(String ruleName) {
        return policyRepo.findByTargetTypeAndActivityTypeAndRuleNameAndActiveTrue(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.STAFF_EVALUATION,
                        ruleName
                )
                .map(MultiplierPolicy::getMultiplier)
                .orElse(1.0);
    }

    // =========================================================================
    // ATTENDANCE MULTIPLIER MATCHING POLICY
    // =========================================================================
    private double resolveMultiplier(PolicyTargetTypeEnum target,
                                     PolicyActivityTypeEnum activity,
                                     int percentage) {

        List<MultiplierPolicy> list =
                policyRepo.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        target, activity);

        for (MultiplierPolicy p : list) {
            boolean okMin = percentage >= p.getMinThreshold();
            boolean okMax = p.getMaxThreshold() == null || percentage <= p.getMaxThreshold();
            if (okMin && okMax) return p.getMultiplier();
        }
        return 1.0;
    }
    private String resolveLevelByFinalScore(int finalScore) {

        List<MultiplierPolicy> list =
                policyRepo.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
                        PolicyTargetTypeEnum.MEMBER,
                        PolicyActivityTypeEnum.FINAL_SCORE
                );

        for (MultiplierPolicy p : list) {

            if (p.getConditionType() != PolicyConditionTypeEnum.ABSOLUTE) {
                continue;
            }

            boolean okMin = finalScore >= p.getMinThreshold();
            boolean okMax = p.getMaxThreshold() == null
                    || finalScore <= p.getMaxThreshold();

            if (okMin && okMax) {
                return p.getRuleName();
            }
        }

        throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Missing FINAL_SCORE ABSOLUTE policy mapping"
        );
    }


    //    // =========================================================================
//    // CLASSIFY LEVEL (LEVEL_FULL / NORMAL / LOW...)
//    // =========================================================================
//    private String classifyActivityLevel(double rate) {
//        int percent = (int) (rate * 100);
//
//        List<MultiplierPolicy> list =
//                policyRepo.findByTargetTypeAndActivityTypeAndActiveTrueOrderByMinThresholdAsc(
//                        PolicyTargetTypeEnum.MEMBER,
//                        PolicyActivityTypeEnum.SESSION_ATTENDANCE);
//
//        for (MultiplierPolicy p : list) {
//            if (!p.getRuleName().startsWith("LEVEL_")) continue;
//
//            boolean okMin = percent >= p.getMinThreshold();
//            boolean okMax = p.getMaxThreshold() == null || percent <= p.getMaxThreshold();
//
//            if (okMin && okMax) return p.getRuleName();
//        }
//        return "LEVEL_LOW";
//    }
private int resolveBaseScore(
        PolicyTargetTypeEnum target,
        PolicyActivityTypeEnum activity
) {
    return policyRepo
            .findByTargetTypeAndActivityTypeAndActiveTrue(target, activity)
            .stream()
            .findFirst()
            .map(MultiplierPolicy::getMinThreshold)
            .orElseThrow(() -> new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Missing base score policy for " + activity
            ));
}

    // =========================================================================
    // RECALCULATE CLUB
    // =========================================================================
    @Override
    @Transactional
    public void recalculateForClub(Long clubId, int year, int month) {

        validateMonth(year, month);
        ensureMonthEditable(year, month);

        List<Membership> members = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED));

        members.forEach(m -> calculateMonthlyRecord(m, year, month));
    }

    // =========================================================================
    // RECALCULATE ALL CLUBS
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
    // GET MEMBER SCORE
    // =========================================================================
    @Override
    public double calculateMemberScore(Long userId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(userId)
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
    // GET SCORE DETAIL
    // =========================================================================
    @Override
    public PerformanceDetailResponse calculateMemberScoreDetail(Long userId) {

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        LocalDate now = LocalDate.now();

        MemberMonthlyActivity act =
                autoCreateIfMissing(membership, now.getYear(), now.getMonthValue());

        return PerformanceDetailResponse.builder()
                .baseScore(act.getAttendanceTotalScore() + act.getStaffTotalScore())
                .multiplier(1.0)
                .finalScore(act.getFinalScore())
                .build();
    }

    // =========================================================================
    // GET MONTHLY REPORT OF A MEMBER
    // =========================================================================
    @Override
    public MemberMonthlyActivity getMonthlyActivity(Long userId, int year, int month) {

        validateMonth(year, month);

        Membership membership = membershipRepo
                .findActiveMembershipsByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Active membership not found"));

        return autoCreateIfMissing(membership, year, month);
    }

    // =========================================================================
    // GET CLUB MONTHLY ACTIVITIES  ❗ (Bạn nói thiếu — đã thêm đầy đủ)
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubMonthlyActivities(
            Long clubId, int year, int month) {

        validateMonth(year, month);

        List<Membership> members = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
        );

        return members.stream()
                .map(m -> monthlyRepo
                        .findByMembershipAndYearAndMonth(m, year, month)
                        .orElse(null))        // ❗ KHÔNG auto-create
                .filter(Objects::nonNull)     // ❗ bỏ member chưa có record
                .toList();
    }


    // =========================================================================
    // CLUB RANKING
    // =========================================================================
    @Override
    public List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month) {

        validateMonth(year, month);

        return monthlyRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month)
                .stream()
                .sorted(Comparator.comparing(MemberMonthlyActivity::getFinalScore).reversed())
                .toList();
    }

    // =========================================================================
    // PREVIEW SCORE
    // =========================================================================
    @Override
    public CalculateScoreResponse calculatePreviewScore(Long membershipId) {

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Membership not found"));

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.plusMonths(1).minusDays(1);

        // ================= ATTENDANCE =================
        int totalSessions =
                attendanceRepo.countByMembership_MembershipIdAndSession_DateBetween(
                        membershipId, start, end);

        int presentSessions =
                attendanceRepo.countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                        membershipId,
                        List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                        start, end);

        double rate = totalSessions == 0 ? 0 : (double) presentSessions / totalSessions;

        // 🔑 attendance base từ POLICY
        int attendanceBase = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.ATTENDANCE_BASE
        );

        double attMul = resolveMultiplier(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.SESSION_ATTENDANCE,
                (int) (rate * 100)
        );

        int attTotal = (int) Math.round(attendanceBase * attMul);

        // ================= STAFF =================
        List<StaffPerformance> staffList =
                staffPerformanceRepo.findPerformanceInRange(membershipId, start, end);

        int staffPointPerTask = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.STAFF_POINT
        );

        int staffTotal = staffList.size() * staffPointPerTask;

        // ================= RESPONSE =================
        return CalculateScoreResponse.builder()
                .attendanceBaseScore(attendanceBase)
                .attendanceMultiplier(attMul)
                .attendanceTotalScore(attTotal)

                // staffBaseScore = điểm / 1 lần
                .staffBaseScore(staffPointPerTask)
                .staffTotalScore(staffTotal)

                .finalScore(attTotal + staffTotal)
                .build();
    }

    // =========================================================================
// 11) LIVE ACTIVITY LIST (Không ghi DB – chỉ preview cho FE)
// =========================================================================
    @Override
    public List<MemberMonthlyActivityResponse> calculateLiveActivities(Long clubId) {

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // 🔑 Base scores từ POLICY (1 lần cho cả list)
        int attendanceBase = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.ATTENDANCE_BASE
        );

        int staffPointPerTask = resolveBaseScore(
                PolicyTargetTypeEnum.MEMBER,
                PolicyActivityTypeEnum.STAFF_POINT
        );

        List<Membership> members = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
        );

        return members.stream().map(m -> {

                    Long membershipId = m.getMembershipId();

                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end   = start.plusMonths(1).minusDays(1);

                    // ================= ATTENDANCE =================
                    int totalSessions = attendanceRepo
                            .countByMembership_MembershipIdAndSession_DateBetween(
                                    membershipId, start, end);

                    int presentSessions = attendanceRepo
                            .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                                    membershipId,
                                    List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                                    start, end
                            );

                    double attendanceRate = totalSessions == 0 ? 0 :
                            (double) presentSessions / totalSessions;

                    double attMul = resolveMultiplier(
                            PolicyTargetTypeEnum.MEMBER,
                            PolicyActivityTypeEnum.SESSION_ATTENDANCE,
                            (int) (attendanceRate * 100)
                    );

                    int attendanceTotal = (int) Math.round(attendanceBase * attMul);

                    // ================= STAFF =================
                    List<StaffPerformance> staffList =
                            staffPerformanceRepo.findPerformanceInRange(membershipId, start, end);

                    int staffTotal = staffList.size() * staffPointPerTask;

                    // ================= FINAL =================
                    int finalScore = attendanceTotal + staffTotal;

                    return MemberMonthlyActivityResponse.builder()
                            .membershipId(m.getMembershipId())
                            .userId(m.getUser().getUserId())
                            .fullName(m.getUser().getFullName())
                            .studentCode(m.getUser().getStudentCode())

                            .clubId(m.getClub().getClubId())
                            .clubName(m.getClub().getName())

                            .year(year)
                            .month(month)

                            .totalEventRegistered(0)
                            .totalEventAttended(0)
                            .eventAttendanceRate(0)
                            .totalPenaltyPoints(0)

                            .activityLevel(resolveLevelByFinalScore(finalScore))

                            .attendanceBaseScore(attendanceBase)
                            .attendanceMultiplier(attMul)
                            .attendanceTotalScore(attendanceTotal)

                            .staffBaseScore(staffPointPerTask)   // 50 điểm / lần
                            .staffMultiplier(1.0)                // không dùng nữa
                            .staffTotalScore(staffTotal)
                            .totalStaffCount(staffList.size())

                            .totalClubSessions(totalSessions)
                            .totalClubPresent(presentSessions)
                            .sessionAttendanceRate(attendanceRate)

                            .finalScore(finalScore)
                            .build();

                }).sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .toList();
    }


}
