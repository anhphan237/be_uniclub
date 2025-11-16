package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffPerformanceMonthlySummaryResponse {

    private Long clubId;
    private int year;
    private int month;

    private long excellentPerformance;
    private long goodPerformance;
    private long averagePerformance;
    private long poorPerformance;

    public static StaffPerformanceMonthlySummaryResponse from(
            Long clubId,
            int year,
            int month,
            long excellent,
            long good,
            long average,
            long poor
    ) {
        return StaffPerformanceMonthlySummaryResponse.builder()
                .clubId(clubId)
                .year(year)
                .month(month)
                .excellentPerformance(excellent)
                .goodPerformance(good)
                .averagePerformance(average)
                .poorPerformance(poor)
                .build();
    }
}

