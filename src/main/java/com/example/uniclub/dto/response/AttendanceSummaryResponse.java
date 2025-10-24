package com.example.uniclub.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private int year;
    private List<MonthlyAttendance> monthlySummary;
    private Long clubId;
    private Long eventId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyAttendance {
        private String month; // e.g. "2025-10"
        private long participantCount;
    }
}