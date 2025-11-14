// NEW
package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.MemberActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberActivityServiceImpl implements MemberActivityService {

    private final ClubRepository clubRepo;
    private final EventRepository eventRepo;
    private final MembershipRepository membershipRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final ClubAttendanceSessionRepository sessionRepo;
    private final ClubAttendanceRecordRepository clubAttendanceRecordRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;
    private final ClubPenaltyRepository penaltyRepo;
    private final MemberMonthlyActivityRepository activityRepo;

    @Override
    @Transactional
    public void recalculateForAllClubsAndMonth(YearMonth month) {
        List<Club> clubs = clubRepo.findAll();
        for (Club club : clubs) {
            recalculateForClubAndMonth(club.getClubId(), month);
        }
    }

    @Override
    @Transactional
    public void recalculateForClubAndMonth(Long clubId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        String monthStr = month.toString(); // "2025-11"

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found: " + clubId));

        // 1️⃣ Event COMPLETED trong tháng
        List<Event> events = eventRepo.findCompletedEventsOfClubInRange(clubId, startDate, endDate);
        List<Long> eventIds = events.stream().map(Event::getEventId).toList();
        int totalEvents = events.size();

        // 2️⃣ Session sinh hoạt trong tháng
        List<ClubAttendanceSession> sessions = sessionRepo.findByClub_ClubIdAndDateBetween(clubId, startDate, endDate);
        int totalSessions = sessions.size();

        // 3️⃣ Membership ACTIVE / APPROVED
        List<Membership> memberships = membershipRepo.findByClub_ClubIdAndStateIn(
                clubId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
        );
        if (memberships.isEmpty()) {
            log.info("No active memberships for club {} in month {}", club.getName(), monthStr);
            return;
        }

        List<MemberMonthlyActivity> calculated = new ArrayList<>();

        for (Membership membership : memberships) {
            Long membershipId = membership.getMembershipId();
            Long userId = membership.getUser().getUserId();

            // ================== EVENT PARTICIPATION ==================
            double eventRate = 0.0;
            int attendedEvents = 0;

            if (totalEvents > 0 && !eventIds.isEmpty()) {
                attendedEvents = (int) attendanceRepo.countByUserAndEventsAndLevels(
                        userId,
                        eventIds,
                        List.of(AttendanceLevelEnum.FULL, AttendanceLevelEnum.HALF)
                );
                eventRate = (double) attendedEvents / totalEvents;
            }

            // ================== DAILY SESSION RATE ===================
            double sessionRate = 0.0;
            int attendedSessions = 0;

            if (totalSessions > 0) {
                attendedSessions = clubAttendanceRecordRepo
                        .countByMembership_MembershipIdAndStatusInAndSession_DateBetween(
                                membershipId,
                                List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                                startDate,
                                endDate
                        );
                sessionRate = (double) attendedSessions / totalSessions;
            }

            // ================== STAFF PERFORMANCE ====================
            double staffScore = 0.0;
            List<StaffPerformance> staffPerformances =
                    staffPerformanceRepo.findByMembership_MembershipIdAndEvent_DateBetween(
                            membershipId,
                            startDate,
                            endDate
                    );
            if (!staffPerformances.isEmpty()) {
                double sum = 0.0;
                for (StaffPerformance sp : staffPerformances) {
                    sum += mapPerformanceToScore(sp.getPerformance());
                }
                staffScore = sum / staffPerformances.size(); // 0–1
            }

            // ================== PENALTY POINTS =======================
            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.plusDays(1).atStartOfDay();

            List<ClubPenalty> penalties = penaltyRepo.findByMembership_MembershipIdAndCreatedAtBetween(
                    membershipId,
                    from,
                    to
            );
            int totalPenaltyPoints = penalties.stream()
                    .mapToInt(p -> p.getPoints() == null ? 0 : p.getPoints())
                    .sum(); // điểm âm

            // ================== RAW SCORE ============================
            double rawScore = calcRawScore(eventRate, sessionRate, staffScore, totalPenaltyPoints);

            // ================== LEVEL & MULTIPLIER ===================
            MemberActivityLevelEnum level = mapLevelFromRawScore(rawScore);
            double multiplier = mapMultiplierFromLevel(level);

            MemberMonthlyActivity activity = activityRepo
                    .findByMembership_MembershipIdAndMonth(membershipId, monthStr)
                    .orElseGet(() -> MemberMonthlyActivity.builder()
                            .membership(membership)
                            .month(monthStr)
                            .build());

            activity.setTotalEvents(totalEvents);
            activity.setAttendedEvents(attendedEvents);
            activity.setEventParticipationRate(eventRate);

            activity.setTotalSessions(totalSessions);
            activity.setAttendedSessions(attendedSessions);
            activity.setSessionRate(sessionRate);

            activity.setStaffScore(staffScore);
            activity.setPenaltyPoints(totalPenaltyPoints);

            activity.setRawScore(rawScore);
            activity.setActivityLevel(level);
            activity.setActivityMultiplier(multiplier);
            activity.setCalculatedAt(LocalDateTime.now());

            activityRepo.save(activity);

            // cập nhật memberMultiplier trên Membership
            membership.setMemberMultiplier(multiplier);
            membershipRepo.save(membership);

            calculated.add(activity);
        }

        // 4️⃣ Chọn "member của tháng" (LEVEL FULL cao nhất)
        assignMemberOfMonth(calculated);
    }

    // ------------------------------------------------------
    //  MAPPING & HELPER
    // ------------------------------------------------------
    private double mapPerformanceToScore(PerformanceLevelEnum p) {
        return switch (p) {
            case POOR -> 0.0;
            case AVERAGE -> 0.4;
            case GOOD -> 0.8;
            case EXCELLENT -> 1.0;
        };
    }

    private double calcRawScore(double eventRate, double sessionRate, double staffScore, int penaltyPoints) {
        // penaltyPoints là số âm (vd: -25)
        double penaltyFactor = 0.0;
        if (penaltyPoints < 0) {
            // mỗi -25 điểm tương đương -0.1, tối đa -0.4
            penaltyFactor = Math.max(-0.4, (penaltyPoints / -25.0) * -0.1);
        }

        // Weighted:
        // 50% event, 25% daily session, 20% staff, 5% penalty
        double score = 0.0;
        score += eventRate * 0.5;
        score += sessionRate * 0.25;
        score += staffScore * 0.2;
        score += penaltyFactor; // penaltyFactor là số âm

        // Clamp 0–1
        if (score < 0) score = 0;
        if (score > 1) score = 1;
        return score;
    }

    private MemberActivityLevelEnum mapLevelFromRawScore(double raw) {
        if (raw < 0.4) return MemberActivityLevelEnum.LOW;
        if (raw < 0.7) return MemberActivityLevelEnum.NORMAL;
        if (raw < 0.9) return MemberActivityLevelEnum.POSITIVE;
        return MemberActivityLevelEnum.FULL;
    }

    private double mapMultiplierFromLevel(MemberActivityLevelEnum level) {
        return switch (level) {
            case LOW -> 1.0;
            case NORMAL -> 1.1;
            case POSITIVE -> 1.2;
            case FULL -> 1.4;
            case MEMBER_OF_MONTH -> 1.6;
        };
    }

    private void assignMemberOfMonth(List<MemberMonthlyActivity> list) {
        if (list.isEmpty()) return;

        Optional<MemberMonthlyActivity> topOpt = list.stream()
                .filter(a -> a.getActivityLevel() == MemberActivityLevelEnum.FULL)
                .max(Comparator
                        .comparing(MemberMonthlyActivity::getRawScore)
                        .thenComparing(MemberMonthlyActivity::getAttendedEvents)
                );

        if (topOpt.isEmpty()) return;

        MemberMonthlyActivity top = topOpt.get();
        Membership membership = top.getMembership();

        top.setActivityLevel(MemberActivityLevelEnum.MEMBER_OF_MONTH);
        top.setActivityMultiplier(1.6);
        activityRepo.save(top);

        membership.setMemberMultiplier(1.6);
        membershipRepo.save(membership);

        log.info("Member of month for club {} in {}: {}",
                membership.getClub().getName(),
                top.getMonth(),
                membership.getUser().getFullName());
    }
}
