package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClubPointsRankingDTO {
    private Long clubId;
    private String clubName;
    private Long totalPoints;
}
