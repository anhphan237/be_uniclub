package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class SystemOverviewResponse {

    private long totalClubs;
    private long totalActiveMembers;
    private long totalEvents;
    private long completedEvents;

    private long monthlyRewardPoints;
    private long totalTransactions;

    private double avgClubFinalScore;
    private double avgEventCheckInRate;
}
