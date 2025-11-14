package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivityDetailResponse {

    private Long membershipId;
    private Long clubId;
    private String clubName;
    private String month;

    private Long userId;
    private String studentCode;
    private String fullName;
    private String email;

    private String memberLevel;
    private String activityLevel;
    private Double activityMultiplier;

    private Integer totalEvents;
    private Integer attendedEvents;
    private Double eventParticipationRate;

    private Integer totalSessions;
    private Integer attendedSessions;
    private Double sessionRate;

    private Double staffScore;
    private Integer penaltyPoints;
    private Double rawScore;
}
