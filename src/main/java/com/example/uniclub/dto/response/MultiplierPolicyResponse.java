package com.example.uniclub.dto.response;

import com.example.uniclub.enums.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicyResponse {

    private Long id;

    private PolicyTargetTypeEnum targetType;

    private String levelEvaluation;

    private PolicyActivityTypeEnum activityType;
    private String ruleName;
    private PolicyConditionTypeEnum conditionType;

    private Integer minThreshold;
    private Integer maxThreshold;

    private Double multiplier;
    private boolean active;

    private String updatedBy;
    private String policyDescription;
}
