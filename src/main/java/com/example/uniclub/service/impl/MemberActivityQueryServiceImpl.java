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
        String monthStr = month.toString(); // "YYYY-MM"
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

                .memberLevel(base.getMemberLevel())             // hiện tại sẽ là null
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
                .rawScore(base.getRawScore())                   // dùng baseScore làm rawScore
                .build();
    }

    // =========================================================
    // 3) ADMIN / STAFF VIEW – ranking giữa các CLB trong 1 tháng
    // =========================================================
    @Override
    public List<ClubActivityRankingItemResponse> getClubRanking(YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        List<MemberMonthlyActivity> all = activityRepo.findByYearAndMonth(year, monthValue);
        if (all.isEmpty()) return List.of();

        // group by club
        Map<Club, List<MemberMonthlyActivity>> byClub = all.stream()
                .collect(Collectors.groupingBy(a -> a.getMembership().getClub()));

        List<ClubActivityRankingItemResponse> result = new ArrayList<>();

        for (Map.Entry<Club, List<MemberMonthlyActivity>> entry : byClub.entrySet()) {
            Club club = entry.getKey();
            List<MemberMonthlyActivity> list = entry.getValue();

            int count = list.size();

            // avg rawScore = avg baseScore
            double avgRaw = list.stream()
                    .map(MemberMonthlyActivity::getBaseScore)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);

            // avg event participation rate
            double avgEvent = list.stream()
                    .mapToDouble(a -> {
                        Integer reg = a.getTotalEventRegistered();
                        Integer att = a.getTotalEventAttended();
                        if (reg == null || reg == 0 || att == null) return 0.0;
                        return (double) att / reg;
                    })
                    .average().orElse(0.0);

            // avg session rate
            double avgSession = list.stream()
                    .mapToDouble(a -> {
                        Integer total = a.getTotalClubSessions();
                        Integer present = a.getTotalClubPresent();
                        if (total == null || total == 0 || present == null) return 0.0;
                        return (double) present / total;
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

        // sort desc by avgRawScore (đúng với DTO hiện tại)
        result.sort(Comparator.comparing(ClubActivityRankingItemResponse::getAvgRawScore).reversed());
        return result;
    }

    // ================= HELPER =================

    private MemberActivitySummaryResponse mapToMemberSummary(MemberMonthlyActivity activity) {
        Membership m = activity.getMembership();
        User u = m.getUser();

        MemberActivityLevelEnum actLevel = activity.getActivityLevel();

        int totalEvents = nvl(activity.getTotalEventRegistered());
        int attendedEvents = nvl(activity.getTotalEventAttended());
        int totalSessions = nvl(activity.getTotalClubSessions());
        int attendedSessions = nvl(activity.getTotalClubPresent());

        double eventRate = totalEvents == 0 ? 0.0 : (double) attendedEvents / totalEvents;
        double sessionRate = totalSessions == 0 ? 0.0 : (double) attendedSessions / totalSessions;

        return MemberActivitySummaryResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(u.getUserId())
                .studentCode(u.getStudentCode())
                .fullName(u.getFullName())
                .email(u.getEmail())

                // 🔹 memberLevel: hiện tại bỏ MemberLevelEnum nên để null cho FE tự xử
                .memberLevel(null)

                .activityLevel(actLevel != null ? actLevel.name() : null)
                .activityMultiplier(activity.getAppliedMultiplier())

                .totalEvents(totalEvents)
                .attendedEvents(attendedEvents)
                .eventParticipationRate(round2(eventRate))

                .totalSessions(totalSessions)
                .attendedSessions(attendedSessions)
                .sessionRate(round2(sessionRate))

                .staffScore(activity.getAvgStaffPerformance())
                .penaltyPoints(activity.getTotalPenaltyPoints())
                .rawScore(activity.getBaseScore()) // dùng baseScore làm “rawScore”
                .build();
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
