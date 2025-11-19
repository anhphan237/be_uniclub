package com.example.uniclub.entity;

import com.example.uniclub.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "multiplier_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiplierPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyTargetTypeEnum targetType; // CLUB, MEMBER

    @Column(length = 255)
    private String levelEvaluation;  // UniStaff / Admin tự nhập

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyActivityTypeEnum activityType; // MEMBER_EVENT_PARTICIPATION...

    @Column(nullable = false)
    private String ruleName; // NORMAL, POSITIVE, FULL...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyConditionTypeEnum conditionType;
    // PERCENTAGE or ABSOLUTE

    @Column(nullable = false)
    private Integer minThreshold; // min value

    private Integer maxThreshold; // nullable = không giới hạn

    @Column(nullable = false)
    private Double multiplier; // x1.2, x1.4...

    @Column(nullable = false)
    private boolean active = true;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private LocalDateTime effectiveFrom;

    @Column(length = 500)
    private String policyDescription;
}
