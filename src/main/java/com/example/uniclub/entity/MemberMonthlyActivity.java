package com.example.uniclub.entity;

import com.example.uniclub.enums.MemberActivityLevelEnum;
import com.example.uniclub.enums.StaffEvaluationEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_monthly_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "membership_id", "year", "month"
        }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberMonthlyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month; // 1–12

    // ====== RAW STATISTICS ======
    @Column(nullable = false)
    private int totalEventRegistered;

    @Column(nullable = false)
    private int totalEventAttended;

    @Column(nullable = false)
    private int totalClubSessions;

    @Column(nullable = false)
    private int totalClubPresent;

    @Column(nullable = false)
    private double avgStaffPerformance;   // 0.0 – 1.0

    // NEW: số lần làm staff
    @Column(nullable = false)
    private int totalStaffCount;

    // NEW: đánh giá staff
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffEvaluationEnum staffEvaluation;  // POOR, AVERAGE, GOOD, EXCELLENT

    @Column(nullable = false)
    private int totalPenaltyPoints;

    // ====== NORMALIZED SCORES ======
    @Column(nullable = false)
    private double baseScore;      // 0.0 – 1.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberActivityLevelEnum activityLevel;

    @Column(nullable = false)
    private double appliedMultiplier;

    @Column(nullable = false)
    private double finalScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ====== COMPUTED HELPERS (optional but recommended) ======
    public double getEventAttendanceRate() {
        return totalEventRegistered == 0 ? 0.0 :
                (double) totalEventAttended / totalEventRegistered;
    }

    public double getSessionAttendanceRate() {
        return totalClubSessions == 0 ? 0.0 :
                (double) totalClubPresent / totalClubSessions;
    }

    // ====== Fix for compatibility with controller ======
    public Double getActivityMultiplier() {
        return appliedMultiplier;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
