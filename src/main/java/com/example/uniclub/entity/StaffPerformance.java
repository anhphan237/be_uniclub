// NEW
package com.example.uniclub.entity;

import com.example.uniclub.enums.PerformanceLevelEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_performances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // staff được đánh giá
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_staff_id")
    private EventStaff eventStaff;

    // để query theo membership
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    // để query theo tháng theo event date
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerformanceLevelEnum performance;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
