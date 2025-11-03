package com.example.uniclub.dto.response;

import com.example.uniclub.enums.PolicyTargetTypeEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicyResponse {
    private Long id;
    private PolicyTargetTypeEnum targetType;
    private String levelOrStatus;
    private Integer minEvents;
    private Double multiplier;
    private boolean active;
    private String updatedBy;
}
