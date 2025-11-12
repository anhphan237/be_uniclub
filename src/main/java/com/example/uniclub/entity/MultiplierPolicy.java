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

    // 🎯 Đối tượng áp dụng: MEMBER hoặc CLUB
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyTargetTypeEnum targetType;

    // 📊 Số sự kiện tối thiểu để đạt mức này
    @Column(name = "min_events_for_club")
    private Integer minEventsForClub;


    // 💰 Hệ số nhân điểm thưởng (VD: 1.0 = bình thường, 1.2 = +20%)
    @Column(nullable = false)
    private Double multiplier = 1.0;
    @Column(name = "level_or_status", nullable = false)
    private String levelOrStatus;

    // ⚙️ Còn hiệu lực hay không
    @Column(nullable = false)
    private boolean active = true;

    // 🕒 Thông tin cập nhật
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime effectiveFrom;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "policy_description")
    private String policyDescription;



}
