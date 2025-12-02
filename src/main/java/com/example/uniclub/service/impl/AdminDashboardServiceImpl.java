package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.EventStatusEnum;

import com.example.uniclub.repository.*;
import com.example.uniclub.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final MemberMonthlyActivityRepository mmaRepo;
    private final ClubAttendanceSessionRepository sessionRepo;
    private final EventRegistrationRepository regRepo;
    private final WalletTransactionRepository walletTxRepo;

    // ======================================================================================
    // SUMMARY
    // ======================================================================================
    @Override
    public AdminSummaryResponse getSummary() {
        return AdminSummaryResponse.builder()
                .totalUsers(userRepo.count())
                .totalClubs(clubRepo.count())
                .totalEvents(eventRepo.count())
                .totalRedeems(0L)
                .totalTransactions(walletTxRepo.count())
                .build();
    }

    // ======================================================================================
    // CLUB RANKING
    // ======================================================================================
    @Override
    public List<ClubRankingResponse> getClubRanking(int year, int month) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Club> clubs = clubRepo.findAll();
        List<ClubRankingResponse> results = new ArrayList<>();

        for (Club club : clubs) {

            // Average final score (from monthly activity)
            Double avgFinal = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, month);
            if (avgFinal == null) avgFinal = 0.0;

            // Completed events count
            int completedEvents = eventRepo.countCompletedInRange(
                    club.getClubId(), EventStatusEnum.COMPLETED, start, end
            );

            // Average event check-in rate
            Double checkRate = eventRepo.avgCheckinRateByClub(club.getClubId());
            if (checkRate == null) checkRate = 0.0;

            // Club attendance sessions within date range
            int sessionCount = sessionRepo.findByClub_ClubIdAndDateBetween(
                    club.getClubId(), start, end
            ).size();

            // Heat Score (prototype formula)
            double heatScore = (avgFinal * 0.4)
                    + (completedEvents * 5)
                    + (sessionCount * 2)
                    + (checkRate * 30);

            results.add(
                    ClubRankingResponse.builder()
                            .clubId(club.getClubId())
                            .clubName(club.getName())
                            .memberCount(club.getMemberCount())
                            .avgFinalScore(avgFinal)
                            .completedEvents(completedEvents)
                            .totalSessions(sessionCount)
                            .avgCheckInRate(checkRate)
                            .heatScore(Math.min(100, heatScore))
                            .build()
            );
        }

        results.sort(Comparator.comparing(ClubRankingResponse::getHeatScore).reversed());
        return results;
    }

    // ======================================================================================
    // EVENT RANKING
    // ======================================================================================
    @Override
    public List<EventRankingResponse> getEventRanking(Integer year, Integer month) {

        List<Event> events;

        if (year != null && month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            // Note: findCompletedEventsForClub(null...) was a placeholder.
            // Ideally, you should create a repository method to fetch ALL completed events within a range.
            events = eventRepo.findAllSettledEvents();
        } else {
            events = eventRepo.findAllSettledEvents();
        }

        List<EventRankingResponse> list = new ArrayList<>();

        for (Event e : events) {

            int registrations = regRepo.countByEvent_EventId(e.getEventId());
            int checkins = e.getCurrentCheckInCount();

            double checkRate = 0;
            if (e.getMaxCheckInCount() != null && e.getMaxCheckInCount() > 0) {
                checkRate = (double) checkins / e.getMaxCheckInCount();
            }

            // Number of staff = number of assigned days OR staff list (if exists)
            int staffCount = (e.getDays() != null ? e.getDays().size() : 0);

            // Popularity score formula (prototype)
            double score = (registrations * 0.4)
                    + (checkRate * 40)
                    + (staffCount * 1.5);

            list.add(
                    EventRankingResponse.builder()
                            .eventId(e.getEventId())
                            .eventName(e.getName())
                            .hostClub(e.getHostClub().getName())
                            .registrations(registrations)
                            .checkInCount(checkins)
                            .checkInRate(checkRate)
                            .staffCount(staffCount)
                            .popularityScore(Math.min(100, score))
                            .build()
            );
        }

        list.sort(Comparator.comparing(EventRankingResponse::getPopularityScore).reversed());
        return list;
    }

    // ======================================================================================
    // ADVANCED OVERVIEW
    // ======================================================================================
    @Override
    public SystemOverviewResponse getAdvancedOverview() {

        long totalClubs = clubRepo.count();
        long totalEvents = eventRepo.count();
        long completedEvents = eventRepo.countByStatus(EventStatusEnum.COMPLETED);
        long activeMembers = userRepo.count();

        return SystemOverviewResponse.builder()
                .totalClubs(totalClubs)
                .totalEvents(totalEvents)
                .completedEvents(completedEvents)
                .totalActiveMembers(activeMembers)
                .monthlyRewardPoints(0L)
                .totalTransactions(walletTxRepo.count())
                .avgClubFinalScore(0)
                .avgEventCheckInRate(0)
                .build();
    }

    // ======================================================================================
    // RECOMMENDATION ENGINE (basic version)
    // ======================================================================================
    @Override
    public List<RecommendationResponse> getRecommendations() {

        List<RecommendationResponse> list = new ArrayList<>();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // Generate last 3 months range
        int m1 = month == 1 ? 12 : month - 1;
        int m2 = (month <= 2) ? (12 - (2 - month)) : month - 2;

        // ================================================
        // 1. CLUB INTELLIGENCE
        // ================================================
        for (Club club : clubRepo.findAll()) {

            Double scoreThis = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, month);
            Double scorePrev = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, m1);
            Double scorePrev2 = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, m2);

            if (scoreThis == null) scoreThis = 0.0;
            if (scorePrev == null) scorePrev = scoreThis;
            if (scorePrev2 == null) scorePrev2 = scorePrev;

            // A. Trend detection (AI heuristic)
            double trend = (scoreThis - scorePrev2) / (scorePrev2 == 0 ? 1 : scorePrev2);

            if (trend < -0.30) {
                list.add(RecommendationResponse.builder()
                        .type("CLUB")
                        .title("Severe performance decline detected")
                        .message("Club '" + club.getName() + "' shows a 30% performance drop over the last 2 months.")
                        .build());
            }

            if (trend > 0.25) {
                list.add(RecommendationResponse.builder()
                        .type("CLUB")
                        .title("Strong performance growth")
                        .message("Club '" + club.getName() + "' is improving rapidly. Consider providing them more activity opportunities.")
                        .build());
            }

            // B. No events this month
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            int eventCount = eventRepo.countCompletedInRange(club.getClubId(),
                    EventStatusEnum.COMPLETED, start, end);

            if (eventCount == 0) {
                list.add(RecommendationResponse.builder()
                        .type("CLUB")
                        .title("Club inactivity detected")
                        .message("Club '" + club.getName() + "' has no completed events this month.")
                        .build());
            }

            // C. Attendance trend for sessions
            int sessions = sessionRepo.findByClub_ClubIdAndDateBetween(club.getClubId(), start, end).size();
            if (sessions == 0) {
                list.add(RecommendationResponse.builder()
                        .type("CLUB")
                        .title("No club sessions recorded")
                        .message("Club '" + club.getName() + "' did not record any sessions this month.")
                        .build());
            }
        }

        // ================================================
        // 2. EVENT INTELLIGENCE
        // ================================================
        for (Event e : eventRepo.findAllSettledEvents()) {

            int regs = regRepo.countByEvent_EventId(e.getEventId());
            int checkin = e.getCurrentCheckInCount();

            // A. High registration but low attendance
            if (regs > 20 && checkin < regs * 0.5) {
                list.add(RecommendationResponse.builder()
                        .type("EVENT")
                        .title("High registration - low attendance anomaly")
                        .message("Event '" + e.getName() + "' has low checkin compared to registrations.")
                        .build());
            }

            // B. Cancel anomaly
            if (e.getStatus() == EventStatusEnum.CANCELLED) {
                list.add(RecommendationResponse.builder()
                        .type("EVENT")
                        .title("Event cancellation")
                        .message("Event '" + e.getName() + "' was cancelled. Review planning or co-club coordination.")
                        .build());
            }
        }

        // ================================================
        // 3. SYSTEM-WIDE INTELLIGENCE
        // ================================================
        long completedEvents = eventRepo.countByStatus(EventStatusEnum.COMPLETED);
        if (completedEvents < 5) {
            list.add(RecommendationResponse.builder()
                    .type("SYSTEM")
                    .title("Low campus activity this month")
                    .message("Event activity across the university is significantly low this month.")
                    .build());
        }

        long totalUsers = userRepo.count();
        if (totalUsers < 200) {
            list.add(RecommendationResponse.builder()
                    .type("SYSTEM")
                    .title("User engagement low")
                    .message("Student engagement is below expectations. Consider a university-wide promotional campaign.")
                    .build());
        }

        return list;
    }
    @Override
    public List<RecommendationResponse> getAIRecommendations() {

        List<RecommendationResponse> list = new ArrayList<>();

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int prev1 = month == 1 ? 12 : month - 1;
        int prev2 = (month <= 2 ? (12 - (2 - month)) : month - 2);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // ========================================================
        // SECTION A – CLUB INTELLIGENCE (HeatScore AI 2.0)
        // ========================================================
        for (Club club : clubRepo.findAll()) {

            Double scoreThis = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, month);
            Double scorePrev = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, prev1);
            Double scorePrev2 = mmaRepo.avgFinalScoreByClub(club.getClubId(), year, prev2);

            if (scoreThis == null) scoreThis = 0.0;
            if (scorePrev == null) scorePrev = scoreThis;
            if (scorePrev2 == null) scorePrev2 = scorePrev;

            double trend = (scoreThis - scorePrev2) / (scorePrev2 == 0 ? 1 : scorePrev2);

            // A1. Big drop
            if (trend < -0.30) {
                list.add(RecommendationResponse.buildClub(
                        "Major performance drop detected",
                        "Club '" + club.getName() +
                                "' shows a 30%+ drop in activity score compared to two months ago. " +
                                "Recommend checking leadership, event planning, or member engagement."
                ));
            }

            // A2. Big improvement
            if (trend > 0.25) {
                list.add(RecommendationResponse.buildClub(
                        "High-performance improvement",
                        "Club '" + club.getName() +
                                "' is surging in activity. Consider giving them more event slots or recognition."
                ));
            }

            // A3. No events
            int eventCount = eventRepo.countCompletedInRange(
                    club.getClubId(), EventStatusEnum.COMPLETED, start, end);

            if (eventCount == 0) {
                list.add(RecommendationResponse.buildClub(
                        "No completed events",
                        "Club '" + club.getName() + "' did not complete any events this month."
                ));
            }

            // A4. No sessions
            int sessions = sessionRepo.findByClub_ClubIdAndDateBetween(
                    club.getClubId(), start, end).size();

            if (sessions == 0) {
                list.add(RecommendationResponse.buildClub(
                        "No attendance sessions",
                        "Club '" + club.getName() + "' recorded no sessions this month."
                ));
            }
        }

        // ========================================================
        // SECTION B – EVENT INTELLIGENCE (Popularity AI 2.0)
        // ========================================================
        for (Event e : eventRepo.findAllSettledEvents()) {

            int regs = regRepo.countByEvent_EventId(e.getEventId());
            int checkin = e.getCurrentCheckInCount();

            if (regs > 30 && checkin < regs * 0.5) {
                list.add(RecommendationResponse.buildEvent(
                        "Attendance anomaly",
                        "Event '" + e.getName() + "' had many registrations (" + regs +
                                ") but low check-ins (" + checkin + "). Consider improving communication."
                ));
            }

            if (e.getStatus() == EventStatusEnum.CANCELLED) {
                list.add(RecommendationResponse.buildEvent(
                        "Event cancellation",
                        "Event '" + e.getName() + "' was cancelled. Review planning/co-club coordination."
                ));
            }
        }

        // ========================================================
        // SECTION C – SYSTEM-WIDE INTELLIGENCE
        // ========================================================
        long completedEvents = eventRepo.countByStatus(EventStatusEnum.COMPLETED);
        if (completedEvents < 5) {
            list.add(RecommendationResponse.buildSystem(
                    "Low event activity month",
                    "The university has low event completion this month."
            ));
        }

        Double globalCheckRate = calculateGlobalCheckInRate();
        if (globalCheckRate != null && globalCheckRate < 0.40) {
            list.add(RecommendationResponse.buildSystem(
                    "University attendance drop",
                    "Average check-in rate across events is falling below 40%."
            ));
        }

        return list;
    }
    /**
     * Calculate the global average check-in rate across all completed events.
     * Returns null if no event has capacity.
     */
    private Double calculateGlobalCheckInRate() {

        List<Event> completedEvents = eventRepo.findAllSettledEvents();

        double totalRate = 0.0;
        int count = 0;

        for (Event e : completedEvents) {
            Integer max = e.getMaxCheckInCount();
            Integer actual = e.getCurrentCheckInCount();

            if (max != null && max > 0) {
                totalRate += (actual * 1.0 / max);
                count++;
            }
        }

        if (count == 0) return null;
        return totalRate / count;   // average check-in rate across all events
    }


}
