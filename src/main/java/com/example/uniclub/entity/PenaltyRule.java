package com.example.uniclub.entity;

import com.example.uniclub.enums.ViolationLevelEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "penalty_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên loại vi phạm
    @Column(nullable = false, unique = true)
    private String name;

    // Mô tả / gợi ý áp dụng
    @Column(columnDefinition = "TEXT")
    private String description;

    // Mức độ vi phạm
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationLevelEnum level;

    // Điểm trừ (luôn âm)
    @Column(nullable = false)
    private Integer penaltyPoints;
}
