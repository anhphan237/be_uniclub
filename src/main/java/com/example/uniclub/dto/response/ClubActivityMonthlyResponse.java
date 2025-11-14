package com.example.uniclub.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubActivityMonthlyResponse {

    private Long clubId;
    private String clubName;
    private String month; // "YYYY-MM"

    private List<MemberActivitySummaryResponse> members;
}
