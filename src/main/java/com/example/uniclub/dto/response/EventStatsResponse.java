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

    private long totalRegistered;

    private long checkinCount;
    private long midCount;
    private long checkoutCount;

    private long noneCount;
    private long halfCount;
    private long fullCount;
    private long suspiciousCount;

    private double participationRate;
    private double midComplianceRate;
    private double fraudRate;
}
