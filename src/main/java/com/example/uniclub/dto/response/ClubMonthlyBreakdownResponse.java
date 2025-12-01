package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubMonthlyBreakdownResponse {

    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    private int totalEvents;
    private double avgFeedback;
    private double avgCheckinRate;

    private double avgMemberActivityScore;
    private double staffPerformanceScore;

    private double finalScore;
    private long rewardPoints;

    // 🔥 BỔ SUNG 2 FIELD BẮT BUỘC — ĐỂ TRÁNH LỖI BUILDER
    private double awardScore;
    private String awardLevel;
}
