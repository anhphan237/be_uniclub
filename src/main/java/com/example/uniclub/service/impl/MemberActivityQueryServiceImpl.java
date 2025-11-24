package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
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
    // 1) CLUB LEADER VIEW – danh sách hoạt động 1 CLB trong tháng
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
    // 2) ADMIN / STAFF – xem chi tiết 1 membership trong tháng
    // =========================================================
    @Override
    public MemberActivityDetailResponse getMemberActivity(Long membershipId, YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        MemberMonthlyActivity a = activityRepo
                .findByMembership_MembershipIdAndYearAndMonth(membershipId, year, monthValue)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No activity data for " + monthStr));

        Membership m = a.getMembership();
        Club club = m.getClub();
        User user = m.getUser();

        // session rate
        double sessionRate = (a.getTotalClubSessions() == 0)
                ? 0.0
                : (double) a.getTotalClubPresent() / a.getTotalClubSessions();

        return MemberActivityDetailResponse.builder()
                .membershipId(m.getMembershipId())
                .clubId(club.getClubId())
                .clubName(club.getName())
                .month(monthStr)

                .userId(user.getUserId())
                .studentCode(user.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())

                // === Attendance ===
                .totalClubSessions(a.getTotalClubSessions())
                .totalClubPresent(a.getTotalClubPresent())
                .sessionAttendanceRate(round2(sessionRate))
                .attendanceBaseScore(a.getAttendanceBaseScore())
                .attendanceMultiplier(a.getAttendanceMultiplier())
                .attendanceTotalScore(a.getAttendanceTotalScore())

                // === Staff ===
                .staffBaseScore(a.getStaffBaseScore())

                .staffTotalScore(a.getStaffTotalScore())

                // === Final score ===
                .finalScore(a.getFinalScore())

                .build();
    }

    // =========================================================
    // 3) ADMIN / STAFF – Ranking CLB theo finalScore trung bình
    // =========================================================
    @Override
    public List<ClubActivityRankingItemResponse> getClubRanking(YearMonth month) {
        String monthStr = month.toString();
        int year = month.getYear();
        int monthValue = month.getMonthValue();

        List<MemberMonthlyActivity> all =
                activityRepo.findByYearAndMonth(year, monthValue);

        if (all.isEmpty()) return List.of();

        // Group theo CLB
        Map<Club, List<MemberMonthlyActivity>> byClub =
                all.stream().collect(Collectors.groupingBy(a -> a.getMembership().getClub()));

        List<ClubActivityRankingItemResponse> result = new ArrayList<>();

        for (Map.Entry<Club, List<MemberMonthlyActivity>> e : byClub.entrySet()) {
            Club club = e.getKey();
            List<MemberMonthlyActivity> list = e.getValue();

            int memberCount = list.size();

            double avgFinal =
                    list.stream().mapToDouble(MemberMonthlyActivity::getFinalScore).average().orElse(0.0);

            double avgSession =
                    list.stream()
                            .mapToDouble(a -> {
                                if (a.getTotalClubSessions() == 0) return 0.0;
                                return (double) a.getTotalClubPresent() / a.getTotalClubSessions();
                            })
                            .average().orElse(0.0);

            result.add(ClubActivityRankingItemResponse.builder()
                    .clubId(club.getClubId())
                    .clubName(club.getName())
                    .month(monthStr)
                    .memberCount(memberCount)
                    .avgRawScore(round2(avgFinal))      // finalScore dùng làm rawScore
                    .avgSessionRate(round2(avgSession))
                    .build());
        }

        // sort theo avgRawScore giảm dần
        result.sort(Comparator.comparing(ClubActivityRankingItemResponse::getAvgRawScore).reversed());
        return result;
    }

    // =========================================================
    // HELPER MAPPING
    // =========================================================
    private MemberActivitySummaryResponse mapToMemberSummary(MemberMonthlyActivity a) {
        Membership m = a.getMembership();
        User u = m.getUser();

        double sessionRate = (a.getTotalClubSessions() == 0)
                ? 0.0
                : (double) a.getTotalClubPresent() / a.getTotalClubSessions();

        return MemberActivitySummaryResponse.builder()
                .membershipId(m.getMembershipId())
                .userId(u.getUserId())
                .studentCode(u.getStudentCode())
                .fullName(u.getFullName())
                .email(u.getEmail())

                // === Attendance ===
                .totalClubSessions(a.getTotalClubSessions())
                .totalClubPresent(a.getTotalClubPresent())
                .sessionAttendanceRate(round2(sessionRate))
                .attendanceBaseScore(a.getAttendanceBaseScore())
                .attendanceMultiplier(a.getAttendanceMultiplier())
                .attendanceTotalScore(a.getAttendanceTotalScore())

                // === Staff ===
                .staffBaseScore(a.getStaffBaseScore())
                .staffTotalScore(a.getStaffTotalScore())

                // === Final ===
                .finalScore(a.getFinalScore())

                .build();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
