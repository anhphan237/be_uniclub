package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClubEventMonthlyActivityResponse {
    private Long clubId;
    private int year;
    private int month;

    private int totalEvents;
    private int completedEvents;
    private int rejectedEvents;

    private String activityLevel;
    private double multiplier;
    private double finalScore;
}
