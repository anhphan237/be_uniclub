package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class EventRankingResponse {
    private Long eventId;
    private String eventName;
    private String hostClub;

    private int registrations;
    private int checkInCount;
    private double checkInRate;

    private int staffCount;
    private double popularityScore; // 0–100
}
