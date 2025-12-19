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

    // ===== EVENT =====
    private int totalEvents;
    private int completedEvents;
    private double eventSuccessRate;

    // ===== ATTENDANCE =====
    private long totalCheckins;

    // ===== FEEDBACK =====
    private long totalFeedbacks;   // ❗ THIẾU
    private double avgFeedback;
}
