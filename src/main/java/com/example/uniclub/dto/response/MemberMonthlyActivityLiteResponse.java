package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberMonthlyActivityLiteResponse {

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

    private int totalPenaltyPoints;
    private int totalStaffCount;

    private int totalClubSessions;
    private int totalClubPresent;
}
