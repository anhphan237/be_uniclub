package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_monthly_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "membership_id", "year", "month"
        })
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;    // 1–12

    // ===========================================================
    //  RAW STATISTICS
    // ===========================================================

    @Column(nullable = false)
    private int totalEventRegistered;

    @Column(nullable = false)
    private int totalEventAttended;

    @Column(nullable = false)
    private int totalClubSessions;

    @Column(nullable = false)
    private int totalClubPresent;

    // Staff counts (from StaffPerformance)
    @Column(nullable = false)
    private int staffGoodCount;

    @Column(nullable = false)
    private int staffAverageCount;

    @Column(nullable = false)
    private int staffPoorCount;

    // ===========================================================
    //  ATTENDANCE SCORE (Excel model)
    // ===========================================================

    @Column(nullable = false)
    private int attendanceBaseScore;

    @Column(nullable = false)
    private double attendanceMultiplier;

    @Column(nullable = false)
    private int attendanceTotalScore;

    // ===========================================================
    //  STAFF SCORE (Excel model)
    // ===========================================================

    @Column(nullable = false)
    private int staffBaseScore;

    @Column(nullable = false)
    private int staffScoreGood;

    @Column(nullable = false)
    private int staffScoreAverage;

    @Column(nullable = false)
    private int staffScorePoor;

    @Column(nullable = false)
    private int staffTotalScore;

    // ===========================================================
    //  FINAL SCORE
    // ===========================================================

    @Column(nullable = false)
    private int finalScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
