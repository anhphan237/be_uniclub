package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivitySummaryResponse {

    private Long membershipId;
    private Long userId;
    private String studentCode;
    private String fullName;
    private String email;

    // ==== Attendance ====
    private int totalClubSessions;
    private int totalClubPresent;
    private double sessionAttendanceRate;
    private int attendanceBaseScore;
    private double attendanceMultiplier;
    private int attendanceTotalScore;

    // ==== Staff ====
    private int staffBaseScore;
    private int staffScoreGood;
    private int staffScoreAverage;
    private int staffScorePoor;
    private int staffTotalScore;

    // Final
    private int finalScore;
}
