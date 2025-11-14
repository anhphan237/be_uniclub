package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.MemberActivityLevelEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.MemberActivityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberActivityQueryServiceImpl implements MemberActivityQueryService {

    private final MemberMonthlyActivityRepository activityRepo;
    private final MembershipRepository membershipRepo;
    private final ClubRepository clubRepo;

    // =========================================================
    // 1) CLUB LEADER VIEW – bảng hoạt động của 1 CLB trong tháng
    // =========================================================
    @Override
    public ClubActivityMonthlyResponse getClubActivity(Long clubId, YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        List<MemberMonthlyActivity> activities =
                activityRepo.findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, monthValue);

        List<MemberActivitySummaryResponse> members = activities.stream()
                .map(this::mapToMemberSummary)
                .collect(Collectors.toList());

        return ClubActivityMonthlyResponse.builder()
                .clubId(club.getClubId())
                .clubName(club.getName())
                .month(monthStr)
                .members(members)
                .build();
    }

    // =========================================================
    // 2) ADMIN / STAFF VIEW – xem chi tiết 1 membership trong tháng
    // =========================================================
    @Override
    public MemberActivityDetailResponse getMemberActivity(Long membershipId, YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        MemberMonthlyActivity activity = activityRepo
                .findByMembership_MembershipIdAndYearAndMonth(membershipId, year, monthValue)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No activity data for this membership in month " + monthStr));

        Membership membership = activity.getMembership();
        Club club = membership.getClub();
        User user = membership.getUser();

        MemberActivitySummaryResponse base = mapToMemberSummary(activity);

        return MemberActivityDetailResponse.builder()
                .membershipId(membership.getMembershipId())
                .clubId(club.getClubId())
                .clubName(club.getName())
                .month(monthStr)
                .userId(user.getUserId())
                .studentCode(user.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())

                .memberLevel(null)             // hệ thống không còn member level
                .activityLevel(base.getActivityLevel())
                .activityMultiplier(base.getActivityMultiplier())

                .totalEvents(base.getTotalEvents())
                .attendedEvents(base.getAttendedEvents())
                .eventParticipationRate(base.getEventParticipationRate())

                .totalSessions(base.getTotalSessions())
                .attendedSessions(base.getAttendedSessions())
                .sessionRate(base.getSessionRate())

                .staffScore(base.getStaffScore())
                .penaltyPoints(base.getPenaltyPoints())
                .rawScore(base.getRawScore())
                .build();
    }

    // =========================================================
    // 3) ADMIN / STAFF VIEW – ranking các CLB
    // =========================================================
    @Override
    public List<ClubActivityRankingItemResponse> getClubRanking(YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        List<MemberMonthlyActivity> all = activityRepo.findByYearAndMonth(year, monthValue);
        if (all.isEmpty()) return List.of();

        Map<Club, List<MemberMonthlyActivity>> byClub = all.stream()
                .collect(Collectors.groupingBy(a -> a.getMembership().getClub()));

        List<ClubActivityRankingItemResponse> result = new ArrayList<>();

        for (Map.Entry<Club, List<MemberMonthlyActivity>> entry : byClub.entrySet()) {
            Club club = entry.getKey();
            List<MemberMonthlyActivity> list = entry.getValue();

            int count = list.size();
            double avgRaw = list.stream()
                    .map(MemberMonthlyActivity::getBaseScore)
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);

            double avgEvent = list.stream()
                    .mapToDouble(a -> {
                        if (a.getTotalEventRegistered() == 0) return 0.0;
                        return (double) a.getTotalEventAttended() / a.getTotalEventRegistered();
                    })
                    .average().orElse(0.0);

            double avgSession = list.stream()
                    .mapToDouble(a -> {
                        if (a.getTotalClubSessions() == 0) return 0.0;
                        return (double) a.getTotalClubPresent() / a.getTotalClubSessions();
                    })
                    .average().orElse(0.0);

            result.add(ClubActivityRankingItemResponse.builder()
                    .clubId(club.getClubId())
                    .clubName(club.getName())
                    .month(monthStr)
                    .memberCount(count)
                    .avgRawScore(round2(avgRaw))
                    .avgEventRate(round2(avgEvent))
                    .avgSessionRate(round2(avgSession))
                    .build());
        }

        result.sort(Comparator.comparing(ClubActivityRankingItemResponse::getAvgRawScore).reversed());
        return result;
    }

    // ================= HELPER =================
    private MemberActivitySummaryResponse mapToMemberSummary(MemberMonthlyActivity a) {
        Membership m = a.getMembership();
        User u = m.getUser();

        return MemberActivitySummaryResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(u.getUserId())
                .studentCode(u.getStudentCode())
                .fullName(u.getFullName())
                .email(u.getEmail())

                .memberLevel(null)
                .activityLevel(a.getActivityLevel().name())
                .activityMultiplier(a.getAppliedMultiplier())

                .totalEvents(a.getTotalEventRegistered())
                .attendedEvents(a.getTotalEventAttended())
                .eventParticipationRate(round2(a.getEventAttendanceRate()))

                .totalSessions(a.getTotalClubSessions())
                .attendedSessions(a.getTotalClubPresent())
                .sessionRate(round2(a.getSessionAttendanceRate()))

                .staffScore(a.getAvgStaffPerformance())
                .penaltyPoints(a.getTotalPenaltyPoints())
                .rawScore(a.getBaseScore())
                .build();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
