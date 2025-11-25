package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class CreateMonthlyActivityRequest {

    private Long membershipId;
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
}
