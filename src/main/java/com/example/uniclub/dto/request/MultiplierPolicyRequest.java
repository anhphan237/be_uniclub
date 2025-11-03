package com.example.uniclub.dto.request;

import com.example.uniclub.enums.PolicyTargetTypeEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicyRequest {
    private PolicyTargetTypeEnum targetType;
    private String levelOrStatus;
    private Integer minEvents;
    private Double multiplier;
    private boolean active;
    private String updatedBy;
}
