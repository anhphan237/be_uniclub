package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "major_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên chuyên ngành
    @Column(nullable = false, unique = true)
    private String majorName;

    // Mô tả chi tiết quy định
    @Column(columnDefinition = "TEXT")
    private String description;

    // Số CLB tối đa mà sinh viên của chuyên ngành này có thể tham gia
    @Column(nullable = false)
    private int maxClubJoin = 3;

    // Điểm thưởng hoặc tiêu chí đặc biệt
    private double rewardMultiplier = 1.0;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private boolean active = true;
}
