package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPolicyResponse {
    private Long id;
    private String policyName;
    private String description;
    private Long majorId;
    private String majorName;
    private Integer maxClubJoin;
    private boolean active;
}
