package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.MemberActivityLevelEnum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberMonthlyActivityResponse {

    private Long membershipId;
    private Long userId;
    private String fullName;
    private String studentCode;
    private Long clubId;
    private String clubName;

    private Integer year;
    private Integer month;

    private int totalEventRegistered;
    private int totalEventAttended;
    private double eventAttendanceRate;

    private int totalClubSessions;
    private int totalClubPresent;
    private double sessionAttendanceRate;

    private double avgStaffPerformance;
    private int totalPenaltyPoints;

    private double baseScore;
    private int baseScorePercent;

    private MemberActivityLevelEnum activityLevel;
    private double appliedMultiplier;
    private double finalScore;

    public static MemberMonthlyActivityResponse from(MemberMonthlyActivity m) {
        Membership membership = m.getMembership();

        return MemberMonthlyActivityResponse.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .fullName(membership.getUser().getFullName())
                .studentCode(membership.getUser().getStudentCode())
                .clubId(membership.getClub().getClubId())
                .clubName(membership.getClub().getName())

                .year(m.getYear())
                .month(m.getMonth())

                .totalEventRegistered(m.getTotalEventRegistered())
                .totalEventAttended(m.getTotalEventAttended())
                .eventAttendanceRate(m.getEventAttendanceRate())

                .totalClubSessions(m.getTotalClubSessions())
                .totalClubPresent(m.getTotalClubPresent())
                .sessionAttendanceRate(m.getSessionAttendanceRate())

                .avgStaffPerformance(m.getAvgStaffPerformance())
                .totalPenaltyPoints(m.getTotalPenaltyPoints())

                .baseScore(m.getBaseScore())
                .baseScorePercent(m.getBaseScorePercent())

                .activityLevel(m.getActivityLevel())
                .appliedMultiplier(m.getAppliedMultiplier())
                .finalScore(m.getFinalScore())
                .build();
    }
}
