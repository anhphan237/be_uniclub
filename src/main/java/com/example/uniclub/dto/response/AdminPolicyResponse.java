package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MultiplierPolicy;
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
    private String name;               // Tên chính sách
    private String description;        // Mô tả chính sách
    private String targetType;         // CLUB / MEMBER
    private String levelOrStatus;      // EXCELLENT, LEGEND, ...
    private Integer minEventsForClub;  // Số sự kiện tối thiểu
    private Double multiplier;         // Hệ số nhân
    private boolean active;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime effectiveFrom;

    // 🧩 Convert từ Entity sang DTO
    public static AdminPolicyResponse fromEntity(MultiplierPolicy mp) {
        return AdminPolicyResponse.builder()
                .id(mp.getId())
                .name(mp.getPolicyName())
                .description(mp.getPolicyDescription())
                .targetType(mp.getTargetType() != null ? mp.getTargetType().name() : null)
                .levelOrStatus(mp.getLevelOrStatus())
                .minEventsForClub(mp.getMinEventsForClub())
                .multiplier(mp.getMultiplier())
                .active(mp.isActive())
                .updatedBy(mp.getUpdatedBy())
                .updatedAt(mp.getUpdatedAt())
                .effectiveFrom(mp.getEffectiveFrom())
                .build();
    }

    // 🧩 Convert từ DTO sang Entity (khi tạo/sửa)
    public MultiplierPolicy toEntity() {
        return MultiplierPolicy.builder()
                .id(id)
                .policyName(name)
                .policyDescription(description)
                .targetType(targetType != null
                        ? Enum.valueOf(PolicyTargetTypeEnum.class, targetType)
                        : null)
                .levelOrStatus(levelOrStatus)
                .minEventsForClub(minEventsForClub)
                .multiplier(multiplier)
                .active(active)
                .updatedBy(updatedBy)
                .updatedAt(updatedAt)
                .effectiveFrom(effectiveFrom)
                .build();
    }
}
