package com.example.uniclub.entity;

import com.example.uniclub.enums.PolicyTargetTypeEnum;
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

    // 🎯 Loại đối tượng áp dụng: CLUB hoặc MEMBER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyTargetTypeEnum targetType;

    // 🔖 Mức độ (EXCELLENT, ACTIVE, CONTRIBUTOR, LEGEND, ...)
    @Column(nullable = false)
    private String levelOrStatus;

    // 📊 Số sự kiện tối thiểu để đạt mức này
    @Column(nullable = false)
    private Integer minEvents;

    // 💰 Hệ số nhân điểm
    @Column(nullable = false)
    private Double multiplier;

    // 👤 Người cập nhật chính sách
    private String updatedBy;

    // ⏰ Thời gian cập nhật
    private LocalDateTime updatedAt;

    // 🗓️ Ngày hiệu lực (nếu UniStaff muốn set chính sách cho tháng sau)
    private LocalDateTime effectiveFrom;
}
