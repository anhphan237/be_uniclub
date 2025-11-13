package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyConditionTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPolicyResponse {

    private Long id;
    private String policyName;
    private String policyDescription;

    private PolicyTargetTypeEnum targetType;
    private PolicyActivityTypeEnum activityType;

    private String ruleName;

    private PolicyConditionTypeEnum conditionType;

    private Integer minThreshold;
    private Integer maxThreshold;

    private Double multiplier;

    private boolean active;

    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime effectiveFrom;

    // --------------------------
    // ENTITY → DTO
    // --------------------------
    public static AdminPolicyResponse fromEntity(MultiplierPolicy mp) {
        return AdminPolicyResponse.builder()
                .id(mp.getId())
                .policyName(mp.getRuleName())
                .policyDescription(mp.getPolicyDescription())
                .targetType(mp.getTargetType())
                .activityType(mp.getActivityType())
                .ruleName(mp.getRuleName())
                .conditionType(mp.getConditionType())
                .minThreshold(mp.getMinThreshold())
                .maxThreshold(mp.getMaxThreshold())
                .multiplier(mp.getMultiplier())
                .active(mp.isActive())
                .updatedBy(mp.getUpdatedBy())
                .updatedAt(mp.getUpdatedAt())
                .effectiveFrom(mp.getEffectiveFrom())
                .build();
    }

    // --------------------------
    // DTO → ENTITY
    // --------------------------
    public MultiplierPolicy toEntity() {
        return MultiplierPolicy.builder()
                .id(id)
                .targetType(targetType)
                .activityType(activityType)
                .ruleName(ruleName)
                .conditionType(conditionType)
                .minThreshold(minThreshold)
                .maxThreshold(maxThreshold)
                .multiplier(multiplier)
                .active(active)
                .updatedBy(updatedBy)
                .updatedAt(updatedAt)
                .effectiveFrom(effectiveFrom)
                .policyDescription(policyDescription)
                .build();
    }
}
