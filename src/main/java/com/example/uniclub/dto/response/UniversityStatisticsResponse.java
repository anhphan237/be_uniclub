package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityStatisticsResponse {
    private long totalClubs;
    private long totalMembers;
    private long totalPoints;
}
