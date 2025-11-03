package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsResponse {
    private long totalClubsJoined;
    private long totalEventsJoined;
    private long totalPointsEarned;
    private long totalAttendanceDays;
}
