package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventStatsResponse {

    private Long eventId;
    private String eventName;

    private int totalRegistered;

    private int checkinCount;
    private int midCount;
    private int checkoutCount;

    private int noneCount;
    private int halfCount;
    private int fullCount;
    private int suspiciousCount;

    private double participationRate;   // (half + full) / total
    private double midComplianceRate;   // mid / checkin
    private double fraudRate;           // suspicious / total
}
