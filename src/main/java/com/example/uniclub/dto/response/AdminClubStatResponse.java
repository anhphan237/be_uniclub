package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClubStatResponse {

    private Long clubId;
    private String clubName;
    private String leaderName;

    private long memberCount;
    private long totalEvents;
    private long activeEvents;
    private long completedEvents;

    private long walletBalance;
}
