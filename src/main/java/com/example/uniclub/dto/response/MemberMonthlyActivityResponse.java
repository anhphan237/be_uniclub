package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MemberMonthlyActivity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    private int totalClubSessions;
    private int totalClubPresent;

    private double sessionAttendanceRate;

    // Attendance (Excel)
    private int attendanceBaseScore;
    private double attendanceMultiplier;
    private int attendanceTotalScore;

    // Staff
    private int staffBaseScore;
    private int staffGoodCount;
    private int staffAverageCount;
    private int staffPoorCount;
    private int staffScoreGood;
    private int staffScoreAverage;
    private int staffScorePoor;
    private int staffTotalScore;

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

                .totalClubSessions(m.getTotalClubSessions())
                .totalClubPresent(m.getTotalClubPresent())
                .sessionAttendanceRate(
                        m.getTotalClubSessions() == 0 ? 0
                                : ((double) m.getTotalClubPresent() / m.getTotalClubSessions())
                )

                // attendance
                .attendanceBaseScore(m.getAttendanceBaseScore())
                .attendanceMultiplier(m.getAttendanceMultiplier())
                .attendanceTotalScore(m.getAttendanceTotalScore())

                // staff
                .staffBaseScore(m.getStaffBaseScore())
                .staffGoodCount(m.getStaffGoodCount())
                .staffAverageCount(m.getStaffAverageCount())
                .staffPoorCount(m.getStaffPoorCount())

                .staffScoreGood(m.getStaffScoreGood())
                .staffScoreAverage(m.getStaffScoreAverage())
                .staffScorePoor(m.getStaffScorePoor())
                .staffTotalScore(m.getStaffTotalScore())

                .finalScore(m.getFinalScore())
                .build();
    }
}
