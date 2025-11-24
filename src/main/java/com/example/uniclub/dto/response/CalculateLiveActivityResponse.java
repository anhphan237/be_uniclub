package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalculateLiveActivityResponse {

    private Long membershipId;
    private Long userId;
    private String fullName;
    private String studentCode;

    private int attendanceBaseScore;
    private double attendanceMultiplier;
    private int attendanceTotalScore;

    private int staffBaseScore;
    private double staffMultiplier;
    private int staffTotalScore;

    private int finalScore;
}
