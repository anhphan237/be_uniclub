package com.example.uniclub.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPolicyRequest {
    private String policyName;
    private String description;
    private Long majorId;
    private String majorName;
    private Integer maxClubJoin;
    private Double rewardMultiplier;
    private boolean active;
}
