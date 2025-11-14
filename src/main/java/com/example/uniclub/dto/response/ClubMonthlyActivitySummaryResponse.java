package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubMonthlyActivitySummaryResponse {

    private Long clubId;
    private String clubName;

    private Integer year;
    private Integer month;

    private int totalEventsCompleted;
    private long memberCount;
    private long fullMembersCount;

    // thông tin "Member of the Month" (nếu có)
    private MemberMonthlyActivityResponse memberOfMonth;

    // hệ số nhân hiện tại của CLB
    private double clubMultiplier;
}
