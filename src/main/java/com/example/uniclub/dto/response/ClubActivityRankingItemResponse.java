package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubActivityRankingItemResponse {

    private Long clubId;
    private String clubName;
    private String month;

    private Integer memberCount;
    private Double avgRawScore;
    private Double avgEventRate;
    private Double avgSessionRate;
}
