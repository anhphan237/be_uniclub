package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ClubEventContributionResponse {

    private Long eventId;
    private String eventName;

    private double feedback;
    private double checkinRate;

    private double weight; // mức đóng góp vào finalScore
}
