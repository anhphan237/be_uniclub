package com.example.uniclub.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

    // 🔗 Nhiều policy có thể trỏ về 1 major
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_club_join")
    private Integer maxClubJoin;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "major_name", length = 100)
    private String majorName;

    @PrePersist
    @PreUpdate
    private void syncMajorName() {
        if (this.major != null) {
            this.majorName = this.major.getName();
        }
    }
}
