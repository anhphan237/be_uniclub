package com.example.uniclub.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMonthlyActivityResponse {

    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    // ===== ACTIVITY METRICS =====
    private int totalEvents;              // số event tổ chức
    private double eventSuccessRate;      // tỉ lệ event hoàn thành
    private double avgFeedback;            // feedback trung bình

    // ===== SCORE =====
    private double finalScore;             // điểm tổng kết
    private double awardScore;             // điểm nhân quy mô
    private String awardLevel;

    private long rewardPoints;             // điểm thưởng CLB nhận

    // ===== LOCK STATUS =====
    private boolean locked;
    private LocalDateTime lockedAt;
    private String lockedBy;
}
