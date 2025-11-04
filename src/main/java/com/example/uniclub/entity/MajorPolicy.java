package com.example.uniclub.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "major_policies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_major_policies_major_id", columnNames = "major_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Mỗi major chỉ có 1 policy
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "major_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_major_policy_major"))
    @JsonBackReference
    private Major major;


    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ⚠️ Không set mặc định trong code; UniStaff cấu hình hết
    @Column(name = "max_club_join", nullable = false)
    private Integer maxClubJoin;

    @Column(nullable = false)
    private boolean active;

    // 📝 Lưu kèm tên ngành để hiển thị nhanh (denormalized),
    // sẽ đồng bộ từ Major ở lifecycle callbacks phía dưới
    @Column(name = "major_name", nullable = false, length = 100)
    private String majorName;

    @PrePersist
    @PreUpdate
    private void syncMajorName() {
        if (this.major == null) {
            throw new IllegalStateException("MajorPolicy.major must not be null");
        }
        // luôn đồng bộ theo Major hiện tại để tránh lệch dữ liệu
        this.majorName = this.major.getName();
    }
}
