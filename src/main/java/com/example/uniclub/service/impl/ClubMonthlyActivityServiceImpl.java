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

    private LocalDate[] calcRange(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return new LocalDate[]{start, end};
    }

    private double normalize(int value, int max) {
        return Math.min((value * 1.0) / max, 1.0) * 100.0;
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
                .avgFeedback(m.getAvgFeedback())
                .avgCheckinRate(m.getAvgCheckinRate())
                .avgMemberActivityScore(m.getAvgMemberActivityScore())
                .staffPerformanceScore(m.getStaffPerformanceScore())

                .finalScore(m.getFinalScore())
                .awardScore(m.getAwardScore())
                .awardLevel(m.getAwardLevel())

                .rewardPoints(m.getRewardPoints())

                .locked(m.isLocked())
                .lockedAt(m.getLockedAt())
                .lockedBy(m.getLockedBy())

                .build();
    }

    private ClubMonthlyBreakdownResponse mapBreakdown(ClubMonthlyActivity m) {
        return ClubMonthlyBreakdownResponse.builder()
                .clubId(m.getClub().getClubId())
                .clubName(m.getClub().getName())
                .year(m.getYear())
                .month(m.getMonth())
                .totalEvents(m.getTotalEvents())
                .avgFeedback(m.getAvgFeedback())
                .avgCheckinRate(m.getAvgCheckinRate())
                .avgMemberActivityScore(m.getAvgMemberActivityScore())
                .staffPerformanceScore(m.getStaffPerformanceScore())
                .finalScore(m.getFinalScore())
                .awardScore(m.getAwardScore())
                .awardLevel(m.getAwardLevel())
                .rewardPoints(m.getRewardPoints())
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

        ClubMonthlyActivity existing = clubMonthlyRepo
                .findByClubAndYearAndMonth(club, year, month)
                .orElse(null);

        if (existing != null && existing.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This month is locked. Cannot recalculate.");
        }

        LocalDate[] range = calcRange(year, month);
        LocalDate start = range[0];
        LocalDate end = range[1];

        // ==================== EVENT METRICS ====================
        int totalEvents = eventRepo.countCompletedInRange(
                clubId, EventStatusEnum.COMPLETED, start, end);

        Double avgFeedback = Optional.ofNullable(
                feedbackRepo.avgRatingByClub(clubId, start, end)
        ).orElse(0.0);

        Double avgCheckinRate = Optional.ofNullable(
                eventRepo.avgCheckinRateByClub(clubId)
        ).orElse(0.0);

        // ================== MEMBER ACTIVITY ====================
        Double avgMemberScore = Optional.ofNullable(
                memberMonthlyRepo.avgFinalScoreByClub(clubId, year, month)
        ).orElse(0.0);

        // ================== STAFF PERFORMANCE ==================
        Double staffScore = Optional.ofNullable(
                staffPerformanceRepo.avgPerformanceByClub(clubId, start, end)
        ).orElse(0.0);

        // ================== FINAL SCORE ========================
        double finalScore =
                0.30 * normalize(totalEvents, 10) +
                        0.25 * (avgFeedback * 20) +
                        0.15 * (avgCheckinRate * 100) +
                        0.20 * avgMemberScore +
                        0.10 * staffScore;

        // =====================================================================
        // 1) MEMBER COUNT → Capacity Factor
        // =====================================================================
        int memberCount = membershipRepo
                .findByClub_ClubIdAndStateIn(
                        clubId, List.of(MembershipStateEnum.ACTIVE)
                ).size();

        double capacityFactor = policyService.resolveMultiplier(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_MEMBER_SIZE,
                memberCount
        );

        // =====================================================================
        // 2) ACTIVITY QUALITY → based on finalScore
        // =====================================================================
        double activityQualityFactor = policyService.resolveMultiplier(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_ACTIVITY_QUALITY,
                (int) Math.round(finalScore)
        );

        // =====================================================================
        // 3) AWARD SCORE
        // =====================================================================
        double awardScore = finalScore * capacityFactor * activityQualityFactor;

        // =====================================================================
        // 4) AWARD LEVEL
        // =====================================================================
        String awardLevel = policyService.resolveRuleName(
                PolicyTargetTypeEnum.CLUB,
                PolicyActivityTypeEnum.CLUB_AWARD_LEVEL,
                (int) Math.round(awardScore)
        );

        // =====================================================================
        // 5) REWARD POINTS → SỐ ĐIỂM THẬT CHO CLB
        // =====================================================================
        long rewardPoints = Math.round(awardScore * 50); // baseRate 50

        // ====================== SAVE RECORD =====================
        ClubMonthlyActivity record = existing;
        if (record == null) {
            record = ClubMonthlyActivity.builder()
                    .club(club)
                    .year(year)
                    .month(month)
                    .build();
        }

        record.setTotalEvents(totalEvents);
        record.setAvgFeedback(avgFeedback);
        record.setAvgCheckinRate(avgCheckinRate);
        record.setAvgMemberActivityScore(avgMemberScore);
        record.setStaffPerformanceScore(staffScore);
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

        return mapBreakdown(record);
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

        LocalDate[] range = calcRange(year, month);
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Monthly record not found"));

        if (record.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This month is already locked.");
        }

        long rewardPoints = record.getRewardPoints();

        // ==============================
        // 1. Wallet
        // ==============================
        Wallet wallet = club.getWallet();
        if (wallet == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Club has no wallet assigned");
        }

        long newBalance = wallet.getBalancePoints() + rewardPoints;
        wallet.setBalancePoints(newBalance);
        walletRepo.save(wallet);

        // ==============================
        // 2. Lock month
        // ==============================
        String staffName = getCurrentUser(); // String (email hoặc username)

        record.setLocked(true);
        record.setLockedBy(staffName);
        record.setLockedAt(LocalDateTime.now());
        clubMonthlyRepo.save(record);

        // ==============================
        // 3. Email notify leaders
        // ==============================
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
                            staffName
                    );
                }
            }
        }

        // ==============================
        // 4. Return
        // ==============================
        return ClubRewardApprovalResponse.builder()
                .clubId(clubId)
                .clubName(club.getName())
                .year(year)
                .month(month)
                .rewardPoints(rewardPoints)
                .locked(true)
                .lockedAt(record.getLockedAt())
                .lockedBy(record.getLockedBy())
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

        long clubRewards = record.getRewardPoints();
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

        int totalScore = activities.stream().mapToInt(MemberMonthlyActivity::getFinalScore).sum();
        if (totalScore <= 0) totalScore = 1;

        long totalDistributed = 0;

        // 3. Loop qua từng member
        for (MemberMonthlyActivity m : activities) {

            Membership membership = m.getMembership();
            User member = membership.getUser();

            int memberScore = m.getFinalScore();
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
            long newBalance = memberWallet.getBalancePoints();
            long oldBalance = newBalance - reward;

            emailService.sendMemberRewardEmail(
                    member.getEmail(),
                    club.getName(),
                    month,
                    year,

                    m.getFinalScore(),
                    m.getAttendanceTotalScore(),
                    m.getStaffTotalScore(),
                    m.getTotalClubSessions(),
                    m.getTotalClubPresent(),
                    m.getStaffEvaluation(),

                    (int) reward,
                    (int) oldBalance,
                    (int) newBalance
            );
        }

            log.info("Distributed {} reward points for club {} in {}/{}",
                totalDistributed, club.getName(), month, year);
    }







}
