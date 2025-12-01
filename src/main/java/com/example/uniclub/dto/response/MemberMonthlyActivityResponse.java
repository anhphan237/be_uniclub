package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MemberMonthlyActivity;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MemberMonthlyActivityResponse {

    private Long membershipId;
    private Long userId;
    private String fullName;
    private String studentCode;
    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    private int totalEventRegistered;
    private int totalEventAttended;
    private double eventAttendanceRate;
    private int totalPenaltyPoints;

    private String activityLevel;

    private int attendanceBaseScore;
    private double attendanceMultiplier;
    private int attendanceTotalScore;

    private int staffBaseScore;
    private int totalStaffCount;
    private String staffEvaluation;
    private double staffMultiplier;
    private int staffScore;
    private int staffTotalScore;

    private int totalClubSessions;
    private int totalClubPresent;
    private double sessionAttendanceRate;

    private int finalScore;

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
                .eventAttendanceRate(m.getEventAttendanceRate())
                .totalPenaltyPoints(m.getTotalPenaltyPoints())

                .activityLevel(m.getActivityLevel())

                .attendanceBaseScore(m.getAttendanceBaseScore())
                .attendanceMultiplier(m.getAttendanceMultiplier())
                .attendanceTotalScore(m.getAttendanceTotalScore())

                .staffBaseScore(m.getStaffBaseScore())
                .totalStaffCount(m.getTotalStaffCount())
                .staffEvaluation(m.getStaffEvaluation())
                .staffMultiplier(m.getStaffMultiplier())
                .staffScore(m.getStaffScore())
                .staffTotalScore(m.getStaffTotalScore())

                .totalClubSessions(m.getTotalClubSessions())
                .totalClubPresent(m.getTotalClubPresent())
                .sessionAttendanceRate(m.getSessionAttendanceRate())

                .finalScore(m.getFinalScore())
                .build();
    }
    public static MemberMonthlyActivityLiteResponse toLite(MemberMonthlyActivity m) {
        return MemberMonthlyActivityLiteResponse.builder()
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
                .totalPenaltyPoints(m.getTotalPenaltyPoints())
                .totalStaffCount(m.getTotalStaffCount())

                .totalClubSessions(m.getTotalClubSessions())
                .totalClubPresent(m.getTotalClubPresent())
                .finalScore(m.getFinalScore())
                .build();
    }

}
