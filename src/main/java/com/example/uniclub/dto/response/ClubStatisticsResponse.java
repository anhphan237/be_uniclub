package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubStatisticsResponse {
    private long clubId;
    private String clubName;
    private long totalMembers;
    private long totalPoints;
}
