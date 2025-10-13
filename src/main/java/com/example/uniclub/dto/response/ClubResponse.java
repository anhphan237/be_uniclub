package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String majorPolicyName;
    private String majorName;
    private Long leaderId;
    private String leaderName;
}
