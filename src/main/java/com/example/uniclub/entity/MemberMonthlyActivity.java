// NEW
package com.example.uniclub.entity;

import com.example.uniclub.enums.MemberActivityLevelEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_monthly_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"membership_id", "month"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberMonthlyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // membership thuộc CLB nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    // tháng, dạng "2025-11"
    @Column(nullable = false, length = 7)
    private String month;

    // ================== CHỈ SỐ HOẠT ĐỘNG ======================

    // Event
    @Column(name = "total_events")
    private Integer totalEvents;

    @Column(name = "attended_events")
    private Integer attendedEvents;

    @Column(name = "event_participation_rate")
    private Double eventParticipationRate;

    // Daily session (attendance CLB)
    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "attended_sessions")
    private Integer attendedSessions;

    @Column(name = "session_rate")
    private Double sessionRate;

    // Staff performance (0–1)
    @Column(name = "staff_score")
    private Double staffScore;

    // Tổng điểm phạt (số âm, ví dụ -25)
    @Column(name = "penalty_points")
    private Integer penaltyPoints;

    // ================== KẾT QUẢ CUỐI ======================

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 30)
    private MemberActivityLevelEnum activityLevel;

    @Column(name = "activity_multiplier")
    private Double activityMultiplier;

    @Column(name = "raw_score")
    private Double rawScore;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}
