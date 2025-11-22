package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.MemberActivityLevelEnum;
import com.example.uniclub.enums.StaffEvaluationEnum;
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

    // NEW
    private int totalStaffCount;
    private StaffEvaluationEnum staffEvaluation;

    private MemberActivityLevelEnum activityLevel;
    private double appliedMultiplier;
    private double finalScore;

    public static MemberMonthlyActivityResponse from(MemberMonthlyActivity m) {
        return MemberMonthlyActivityResponse.builder()
                .membershipId(m.getMembership().getMembershipId())
                .userId(m.getMembership().getUser().getUserId())
                .fullName(m.getMembership().getUser().getFullName())
                .studentCode(m.getMembership().getUser().getStudentCode())
                .clubId(m.getMembership().getClub().getClubId())
                .clubName(m.getMembership().getClub().getName())

                .year(m.getYear())
                .month(m.getMonth())

                .totalEventRegistered(m.getTotalEventRegistered())
                .totalEventAttended(m.getTotalEventAttended())
                .eventAttendanceRate(calcRate(m.getTotalEventAttended(), m.getTotalEventRegistered()))

                .totalClubSessions(m.getTotalClubSessions())
                .totalClubPresent(m.getTotalClubPresent())
                .sessionAttendanceRate(calcRate(m.getTotalClubPresent(), m.getTotalClubSessions()))

                .totalStaffCount(m.getTotalStaffCount())
                .staffEvaluation(m.getStaffEvaluation())

                .baseScore(m.getBaseScore())
                .appliedMultiplier(m.getAppliedMultiplier())
                .finalScore(m.getFinalScore())

                .build();
    }

    private static double calcRate(int a, int b) {
        return b == 0 ? 0 : (a * 1.0 / b);
    }
}
