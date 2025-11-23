// NEW
package com.example.uniclub.entity;

import com.example.uniclub.enums.ClubPenaltyTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // thành viên bị phạt
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubPenaltyTypeEnum type;

    // điểm phạt, luôn âm (vd: -5, -10, -30)
    @Column(nullable = false)
    private Integer points;

    @Column(length = 255)
    private String reason;

    // người tạo (leader / vice / staff)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
