package com.example.uniclub.dto.request;

import com.example.uniclub.enums.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicyRequest {

    private PolicyTargetTypeEnum targetType;          // CLUB / MEMBER
    private PolicyActivityTypeEnum activityType;      // MEMBER_EVENT_PARTICIPATION...

    private String ruleName;                          // low / normal / full...
    private PolicyConditionTypeEnum conditionType;    // PERCENTAGE / ABSOLUTE

    private Integer minThreshold;                     // ngưỡng dưới
    private Integer maxThreshold;                     // ngưỡng trên (nullable)

    private Double multiplier;                        // hệ số nhân

    private boolean active;                           // còn hiệu lực?

    private String updatedBy;
    private String policyDescription;
}
