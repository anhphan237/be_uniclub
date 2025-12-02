package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRankingResponse {

    private Integer rank;          // <-- ADD THIS
    private Long clubId;
    private String clubName;

    private Integer memberCount;

    private Double avgFinalScore;
    private Integer completedEvents;
    private Integer totalSessions;
    private Double avgCheckInRate;

    private Double heatScore;

    private Long totalPoints;      // <-- ADD THIS (used for University Points Ranking)
}
