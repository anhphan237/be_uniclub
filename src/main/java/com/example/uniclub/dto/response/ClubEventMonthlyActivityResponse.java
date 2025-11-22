package com.example.uniclub.dto.response;

import com.example.uniclub.enums.ClubEventActivityEnum;
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

    private ClubEventActivityEnum activityLevel;
    private double multiplier;
    private double finalScore;
}
