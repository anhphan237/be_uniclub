package com.example.uniclub.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ClubMonthlyActivityResponse {

    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    private int totalEvents;
    private double avgFeedback;
    private double avgCheckinRate;

    private double avgMemberActivityScore;
    private double staffPerformanceScore;
    private double awardScore;
    private String awardLevel;
    private boolean locked;
    private LocalDateTime lockedAt;
    private String lockedBy;
    private long rewardPoints;

    private double finalScore;
}
