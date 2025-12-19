package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMonthlySummaryResponse {

    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    private int totalEvents;
    private int completedEvents;
    private double successRate;

    private long totalCheckins;
    private double avgFeedback;
}
