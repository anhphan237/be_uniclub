package com.example.uniclub.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MajorPolicyResponse {

    private Long id;

    private String policyName;

    private String description;

    private Long majorId;

    private String majorName; // 🟢 thêm để khớp với entity và service

    private String name;

    private Integer maxClubJoin;

    private Double rewardMultiplier;

    private boolean active;
}
