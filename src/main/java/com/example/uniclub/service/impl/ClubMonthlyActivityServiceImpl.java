package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubMonthlyActivityService;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.MultiplierPolicyService;

import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMonthlyActivityServiceImpl implements ClubMonthlyActivityService {

    private final ClubRepository clubRepo;
    private final ClubMonthlyActivityRepository clubMonthlyRepo;
    private final EventRepository eventRepo;
    private final EventFeedbackRepository feedbackRepo;
    private final MemberMonthlyActivityRepository memberMonthlyRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;
    private final MultiplierPolicyService policyService;
    private final MembershipRepository membershipRepo;
    private final WalletRepository walletRepo;
    private final EmailService emailService;
    private final WalletTransactionRepository walletTransactionRepo;
    private final WalletService walletService;
    // =========================================================================
    // 0. VALIDATE & HELPERS
    // =========================================================================
    private void validate(int year, int month) {
        if (month < 1 || month > 12)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must be 1–12");
        if (year < 2000)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year");
    }

    private LocalDate[] range(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        return new LocalDate[]{start, start.plusMonths(1).minusDays(1)};
    }

    private String operator() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }



    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }



    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null ? auth.getName() : "system");
    }

    // =========================================================================
    //  MAP DTO
    // =========================================================================
    private ClubMonthlyActivityResponse map(ClubMonthlyActivity m) {
        return ClubMonthlyActivityResponse.builder()
                .clubId(m.getClub().getClubId())
                .clubName(m.getClub().getName())

                .year(m.getYear())
                .month(m.getMonth())

                .totalEvents(m.getTotalEvents())
                .eventSuccessRate(m.getAvgCheckinRate()) // bản chất là successRate
                .avgFeedback(m.getAvgFeedback())

                .finalScore(m.getFinalScore())
                .awardScore(m.getAwardScore())
                .awardLevel(m.getAwardLevel())
                .rewardPoints(m.getRewardPoints())

                .locked(m.isLocked())
                .lockedAt(m.getLockedAt())
                .lockedBy(m.getLockedBy())
                .build();
    }



    // =========================================================================
    // 1. RECALCULATE A CLUB
    // =========================================================================
    @Override
    @Transactional
    public ClubMonthlyActivityResponse recalculateForClub(Long clubId, int year, int month) {

        validate(year, month);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElse(null);

        if (record != null && record.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month is locked");
        }

        // ===== DATE RANGE =====
        LocalDate[] r = range(year, month);
        LocalDateTime start = r[0].atStartOfDay();
        LocalDateTime end = r[1].atTime(23, 59, 59);

        // =====================================================
        // 1️⃣ EVENT DATA
        // =====================================================
        int totalEvents = eventRepo.countEventsInRange(clubId, r[0], r[1]);
        int completedEvents = eventRepo.countCompletedInRange(
                clubId, EventStatusEnum.COMPLETED, r[0], r[1]);

        double eventSuccessRate = totalEvents == 0
                ? 0
                : (completedEvents * 1.0 / totalEvents);
        eventSuccessRate = round(eventSuccessRate, 2);

        long totalCheckins = Optional.ofNullable(
                eventRepo.sumTotalCheckinByClubInRange(clubId, r[0], r[1])
        ).orElse(0L);

        double avgCheckinRate = totalEvents == 0
                ? 0
                : (double) totalCheckins / totalEvents;
        avgCheckinRate = round(avgCheckinRate, 2);

        double avgFeedback = Optional.ofNullable(
                feedbackRepo.avgRatingByClub(clubId, start, end)
        ).orElse(0.0);
        avgFeedback = round(avgFeedback, 2);

        // =====================================================
        // 2️⃣ VOLUME SCORE
        // =====================================================
        double volumeScore = totalEvents * 10;

        // =====================================================
        // 3️⃣ QUALITY SCORE
        // =====================================================
        double qualityScore =
                (eventSuccessRate * 100 * 0.4)
                        + (avgFeedback * 20 * 0.4)
                        + (avgCheckinRate * 100 * 0.2);

        qualityScore = round(qualityScore, 2);

        // ❌ PHẠT NẶNG: CÓ EVENT NHƯNG FAIL HẾT
        if (totalEvents > 0 && eventSuccessRate == 0) {
            qualityScore = qualityScore * 0.3;
        }

        // =====================================================
        // 4️⃣ MULTIPLIER
        // =====================================================
        double activityMultiplier = totalEvents == 0
                ? 1
                : policyService.resolveMultiplier(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_EVENT_ACTIVITY,
                totalEvents
        );

        double qualityMultiplier = totalEvents == 0
                ? 1
                : policyService.resolveMultiplier(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_ACTIVITY_QUALITY,
                (int) qualityScore
        );

        // =====================================================
        // 5️⃣ FINAL SCORE
        // =====================================================
        double finalScore = (volumeScore + qualityScore)
                * activityMultiplier
                * qualityMultiplier;

        finalScore = round(finalScore, 2);

        // =====================================================
        // 6️⃣ AWARD SCORE
        // =====================================================
        long memberCount = membershipRepo.countByClub_ClubIdAndState(
                clubId, MembershipStateEnum.ACTIVE);

        double impactFactor = Math.log10(memberCount + 1) + 1;
        double awardScore = round(finalScore * impactFactor, 2);

        // =====================================================
        // 7️⃣ AWARD LEVEL & REWARD
        // =====================================================
        String awardLevel = policyService.resolveRuleName(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_AWARD_LEVEL,
                (int) awardScore
        );

        long rewardPoints = Math.max(0, Math.round(
                awardScore *
                        policyService.resolveMultiplier(
                                PolicyTargetTypeEnum.CLUB,
                                PolicyActivityTypeEnum.CLUB_MEMBER_SIZE,
                                (int) memberCount
                        )
        ));

        // =====================================================
        // 8️⃣ SAVE
        // =====================================================
        if (record == null) {
            record = ClubMonthlyActivity.builder()
                    .club(club)
                    .year(year)
                    .month(month)
                    .build();
        }

        record.setTotalEvents(totalEvents);
        record.setEventSuccessRate(eventSuccessRate);
        record.setAvgCheckinRate(avgCheckinRate);
        record.setAvgFeedback(avgFeedback);

        record.setFinalScore(finalScore);
        record.setAwardScore(awardScore);
        record.setAwardLevel(awardLevel);
        record.setRewardPoints(rewardPoints);

        clubMonthlyRepo.save(record);
        return map(record);
    }






    // =========================================================================
    // 2. RECALCULATE ALL CLUBS
    // =========================================================================
    @Override
    @Transactional
    public List<ClubMonthlyActivityResponse> recalculateAllClubs(int year, int month) {
        validate(year, month);
        List<ClubMonthlyActivityResponse> result = new ArrayList<>();
        for (Club c : clubRepo.findAll()) {
            result.add(recalculateForClub(c.getClubId(), year, month));
        }
        return result;
    }

    // =========================================================================
    // 3. GET CLUB MONTHLY ACTIVITY
    // =========================================================================
    @Override
    public ClubMonthlyActivityResponse getClubMonthlyActivity(Long clubId, int year, int month) {
        validate(year, month);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Monthly record not found"));

        return map(record);
    }

    // =========================================================================
    // 4. RANKING
    // =========================================================================
    @Override
    public List<ClubMonthlyActivityResponse> getClubRanking(int year, int month) {
        validate(year, month);
        return clubMonthlyRepo
                .findByYearAndMonthOrderByFinalScoreDesc(year, month)
                .stream()
                .map(this::map)
                .toList();
    }

    // =========================================================================
    // 5. CHECK EXISTS
    // =========================================================================
    @Override
    public boolean exists(Long clubId, int year, int month) {
        validate(year, month);
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        return clubMonthlyRepo.findByClubAndYearAndMonth(club, year, month).isPresent();
    }

    // =========================================================================
    // 6. DELETE
    // =========================================================================
    @Override
    @Transactional
    public void deleteMonthlyRecord(Long clubId, int year, int month) {
        validate(year, month);
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .ifPresent(clubMonthlyRepo::delete);
    }

    // =========================================================================
    // 7. TRENDING
    // =========================================================================
    @Override
    public List<ClubTrendingResponse> getTrendingClubs(int year, int month) {

        validate(year, month);

        int prevMonth = (month == 1 ? 12 : month - 1);
        int prevYear = (month == 1 ? year - 1 : year);

        List<ClubMonthlyActivityResponse> thisMonth = getClubRanking(year, month);
        List<ClubMonthlyActivityResponse> lastMonth = getClubRanking(prevYear, prevMonth);

        Map<Long, Double> prevMap = new HashMap<>();
        lastMonth.forEach(r -> prevMap.put(r.getClubId(), r.getFinalScore()));

        List<ClubTrendingResponse> result = new ArrayList<>();

        for (ClubMonthlyActivityResponse now : thisMonth) {

            double prevScore = prevMap.getOrDefault(now.getClubId(), 0.0);
            double diff = now.getFinalScore() - prevScore;
            double percent = (prevScore == 0 ? 100 : (diff / prevScore) * 100);

            result.add(ClubTrendingResponse.builder()
                    .clubId(now.getClubId())
                    .clubName(now.getClubName())
                    .currentScore(now.getFinalScore())
                    .previousScore(prevScore)
                    .scoreDiff(diff)
                    .percentGrowth(percent)
                    .build());
        }

        result.sort((a, b) -> Double.compare(b.getScoreDiff(), a.getScoreDiff()));

        return result;
    }

    // =========================================================================
    // 8. HISTORY
    // =========================================================================
    @Override
    public List<ClubMonthlyHistoryPoint> getClubHistory(Long clubId, int year) {

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        List<ClubMonthlyHistoryPoint> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {

            int monthFinal = month;

            clubMonthlyRepo.findByClubAndYearAndMonth(club, year, month)
                    .ifPresent(r -> result.add(
                            ClubMonthlyHistoryPoint.builder()
                                    .month(monthFinal)
                                    .score(r.getFinalScore())
                                    .build()
                    ));
        }
        return result;
    }

    // =========================================================================
    // 9. BREAKDOWN
    // =========================================================================
    @Override
    public ClubMonthlyBreakdownResponse getBreakdown(Long clubId, int year, int month) {

        validate(year, month);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Record not found"));

        return ClubMonthlyBreakdownResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())

                .year(year)
                .month(month)

                // ===== EVENT METRICS =====
                .totalEvents(record.getTotalEvents())
                .avgFeedback(record.getAvgFeedback())
                .avgCheckinRate(record.getAvgCheckinRate())

                // ===== KHÔNG DÙNG TRONG LOGIC CLB → SET 0 =====
                .avgMemberActivityScore(0)
                .staffPerformanceScore(0)

                // ===== FINAL =====
                .finalScore(record.getFinalScore())
                .awardScore(record.getAwardScore())
                .rewardPoints(record.getRewardPoints())
                .awardLevel(record.getAwardLevel())

                .build();
    }

    // =========================================================================
    // 10. COMPARE CLUBS
    // =========================================================================
    @Override
    public ClubCompareResponse compareClubs(Long clubA, Long clubB, int year, int month) {
        return ClubCompareResponse.builder()
                .clubA(getBreakdown(clubA, year, month))
                .clubB(getBreakdown(clubB, year, month))
                .build();
    }

    // =========================================================================
    // 11. EVENT CONTRIBUTION
    // =========================================================================
    @Override
    public List<ClubEventContributionResponse> getEventContribution(Long clubId, int year, int month) {

        validate(year, month);

        LocalDate[] range = range(year, month);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<Event> events = eventRepo.findCompletedEventsForClub(clubId, start, end);

        List<ClubEventContributionResponse> result = new ArrayList<>();

        for (Event e : events) {

            double feedback = Optional.ofNullable(
                    feedbackRepo.avgRatingByEvent(e.getEventId())
            ).orElse(0.0);

            double checkinRate = Optional.ofNullable(
                    (e.getMaxCheckInCount() == null || e.getMaxCheckInCount() == 0)
                            ? null
                            : (e.getCurrentCheckInCount() * 1.0 / e.getMaxCheckInCount())
            ).orElse(0.0);

            double weight =
                    (feedback * 20) * 0.6 +
                            (checkinRate * 100) * 0.4;

            result.add(ClubEventContributionResponse.builder()
                    .eventId(e.getEventId())
                    .eventName(e.getName())
                    .feedback(feedback)
                    .checkinRate(checkinRate)
                    .weight(weight)
                    .build());
        }

        result.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));

        return result;
    }

    // =========================================================================
    // 12. LOCK RECORD
    // =========================================================================
    @Override
    @Transactional
    public ClubMonthlyActivityResponse lockMonthlyRecord(Long clubId, int year, int month) {

        validate(year, month);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Monthly activity not found"));

        if (record.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This month is already locked.");
        }

        // ==========================
        // 1. Lock
        // ==========================
        String locker = getCurrentUser();   // trả về email/username (String)

        record.setLocked(true);
        record.setLockedAt(LocalDateTime.now());
        record.setLockedBy(locker);
        clubMonthlyRepo.save(record);

        // ==========================
        // 2. Email Notification
        // ==========================
        if (club.getMemberships() != null) {
            for (Membership m : club.getMemberships()) {
                if (m.isLeaderRole()) {
                    emailService.sendClubMonthlyLockedEmail(
                            m.getUser().getEmail(),
                            club.getName(),
                            month,
                            year,
                            locker
                    );
                }
            }
        }

        return map(record);
    }





    // =========================================================================
    // 13. APPROVE REWARD POINTS
    // =========================================================================
    @Override
    @Transactional
    public ClubRewardApprovalResponse approveRewardPoints(Long clubId, int year, int month) {

        validate(year, month);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Monthly record not found"));

        // ✅ BẮT BUỘC PHẢI LOCK TRƯỚC
        if (!record.isLocked()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Monthly record must be locked before approval"
            );
        }

        // ✅ KHÔNG DUYỆT 2 LẦN
        if (record.isApproved()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Reward already approved"
            );
        }

        long rewardPoints = record.getRewardPoints();
        if (rewardPoints <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Reward points must be greater than 0"
            );
        }

        Wallet clubWallet = club.getWallet();
        if (clubWallet == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Club has no wallet"
            );
        }

        String operator = getCurrentUser();

        // =====================================================
        // ⭐ 1) UNI → CLUB TOPUP + TRANSACTION LOG
        // =====================================================
        walletService.topupPointsFromUniversityWithOperator(
                clubWallet.getWalletId(),
                rewardPoints,
                "Monthly club reward " + month + "/" + year,
                operator
        );

        long newBalance = clubWallet.getBalancePoints();

        // =====================================================
        // ⭐ 2) MARK APPROVED (KHÔNG ĐỤNG LOCK)
        // =====================================================
        record.setApproved(true);
        record.setApprovedAt(LocalDateTime.now());
        record.setApprovedBy(operator);
        clubMonthlyRepo.save(record);

        // =====================================================
        // ⭐ 3) EMAIL LEADER
        // =====================================================
        if (club.getMemberships() != null) {
            for (Membership m : club.getMemberships()) {
                if (m.isLeaderRole()) {
                    emailService.sendClubMonthlyRewardApprovedEmail(
                            m.getUser().getEmail(),
                            club.getName(),
                            rewardPoints,
                            newBalance,
                            month,
                            year,
                            operator
                    );
                }
            }
        }

        // =====================================================
        // ⭐ 4) RESPONSE
        // =====================================================
        return ClubRewardApprovalResponse.builder()
                .clubId(clubId)
                .clubName(club.getName())
                .year(year)
                .month(month)
                .rewardPoints(rewardPoints)
                .approved(true)
                .approvedBy(operator)
                .approvedAt(record.getApprovedAt())
                .walletBalance(newBalance)
                .build();
    }



    @Override
    @Transactional
    public void distributeRewardToMembers(Long clubId, int year, int month) {

        // 1. Lấy dữ liệu đầu vào
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubMonthlyActivity record = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Monthly record not found"));

        if (!record.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must be locked before distributing.");
        }

        long clubRewards = record.getRewardPoints(); // Long
        if (clubRewards <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club has no reward points.");

        Wallet clubWallet = club.getWallet();
        if (clubWallet.getBalancePoints() < clubRewards)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club wallet does not have enough points.");

        // 2. Lấy danh sách activity member
        List<MemberMonthlyActivity> activities =
                memberMonthlyRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        if (activities.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "No members to reward.");

        int totalScore = activities.stream()
                .mapToInt(MemberMonthlyActivity::getFinalScore)
                .sum();
        if (totalScore <= 0) totalScore = 1;

        long totalDistributed = 0;

        // 3. Loop qua từng member
        for (MemberMonthlyActivity m : activities) {

            Membership membership = m.getMembership();
            User member = membership.getUser();

            int memberScore = m.getFinalScore(); // final score

            // reward = (score / totalScore) * rewardPool
            long reward = (memberScore * clubRewards) / totalScore;
            totalDistributed += reward;

            Wallet memberWallet = walletRepo.findByUser_UserId(member.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Member wallet missing: " + member.getUserId()));

            String reason = "Monthly reward " + month + "/" + year + " from " + club.getName();

            // 3.1 Chuyển điểm CLB -> Member
            walletService.transferPointsWithType(
                    clubWallet,
                    memberWallet,
                    reward,
                    reason,
                    WalletTransactionTypeEnum.CLUB_TO_MEMBER
            );

            // 3.2 Log thưởng riêng
            WalletTransaction bonusTx = WalletTransaction.builder()
                    .wallet(memberWallet)
                    .amount(reward)
                    .type(WalletTransactionTypeEnum.BONUS_REWARD)
                    .receiverUser(member)
                    .receiverMembership(membership)
                    .senderName(club.getName())
                    .receiverName(member.getFullName())
                    .description("BONUS_REWARD for " + month + "/" + year)
                    .createdAt(LocalDateTime.now())
                    .build();

            walletTransactionRepo.save(bonusTx);

            // 3.3 Gửi email
            long newBalanceLong = memberWallet.getBalancePoints();
            long oldBalanceLong = newBalanceLong - reward;

            emailService.sendMemberRewardEmail(
                    member.getEmail(),
                    club.getName(),
                    month,
                    year,

                    memberScore,
                    m.getAttendanceTotalScore(),
                    m.getStaffTotalScore(),
                    m.getTotalClubSessions(),
                    m.getTotalClubPresent(),
                    m.getStaffEvaluation(),

                    totalScore,                   // int
                    (int) clubRewards,            // Long -> int
                    (int) reward,                 // Long -> int
                    (int) oldBalanceLong,         // Long -> int
                    (int) newBalanceLong          // Long -> int
            );
        }

        log.info("Distributed {} reward points for club {} in {}/{}",
                totalDistributed, club.getName(), month, year);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ClubMonthlySummaryResponse> getMonthlySummary(int year, int month) {

        validate(year, month);

        // ===== DATE RANGE (FIX LocalDate vs LocalDateTime) =====
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<ClubMonthlySummaryResponse> result = new ArrayList<>();

        for (Club club : clubRepo.findAll()) {

            Long clubId = club.getClubId();

            // ===== EVENT =====
            int totalEvents = eventRepo.countEventsInRange(clubId, startDate, endDate);
            int completedEvents = eventRepo.countCompletedInRange(
                    clubId, EventStatusEnum.COMPLETED, startDate, endDate
            );

            double eventSuccessRate = totalEvents == 0
                    ? 0
                    : (completedEvents * 1.0 / totalEvents);
            eventSuccessRate = round(eventSuccessRate, 2);

            // ===== ATTENDANCE =====
            long totalCheckins = Optional.ofNullable(
                    eventRepo.sumTotalCheckinByClubInRange(clubId, startDate, endDate)
            ).orElse(0L);

            // ===== FEEDBACK =====
            long totalFeedbacks = feedbackRepo.countByClubInRange(
                    clubId, start, end
            );

            double avgFeedback = Optional.ofNullable(
                    feedbackRepo.avgRatingByClub(clubId, start, end)
            ).orElse(0.0);
            avgFeedback = round(avgFeedback, 2);

            // ===== BUILD RESPONSE =====
            result.add(
                    ClubMonthlySummaryResponse.builder()
                            .clubId(clubId)
                            .clubName(club.getName())
                            .year(year)
                            .month(month)
                            .totalEvents(totalEvents)
                            .completedEvents(completedEvents)
                            .eventSuccessRate(eventSuccessRate)
                            .totalCheckins(totalCheckins)
                            .totalFeedbacks(totalFeedbacks)
                            .avgFeedback(avgFeedback)
                            .build()
            );
        }

        return result;
    }



}
