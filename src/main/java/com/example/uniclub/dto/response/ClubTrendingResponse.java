package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ClubTrendingResponse {

    private Long clubId;
    private String clubName;

    private double currentScore;
    private double previousScore;

    private double scoreDiff;     // tăng/giảm bao nhiêu điểm
    private double percentGrowth; // % tăng trưởng
}
