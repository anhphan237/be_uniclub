package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String majorPolicyName;
    private Long majorId;
    private String majorName;
    private Long leaderId;
    private String leaderName;
    private Long memberCount;
}
