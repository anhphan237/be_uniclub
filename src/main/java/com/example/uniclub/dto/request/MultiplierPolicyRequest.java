package com.example.uniclub.dto.request;

import com.example.uniclub.enums.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicyRequest {

    private PolicyTargetTypeEnum targetType;

    private PolicyActivityTypeEnum activityType;

    private String ruleName;
    private PolicyConditionTypeEnum conditionType;

    private Integer minThreshold;
    private Integer maxThreshold;
    private String levelEvaluation;

    private Double multiplier;

    private boolean active;

    private String updatedBy;
    private String policyDescription;
}
