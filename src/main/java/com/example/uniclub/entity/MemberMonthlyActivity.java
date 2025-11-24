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
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MemberMonthlyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    // ======================
    // EVENT
    // ======================
    @Column(nullable = false)
    private int totalEventRegistered;

    @Column(nullable = false)
    private int totalEventAttended;

    @Column(nullable = false)
    private double eventAttendanceRate;

    @Column(nullable = false)
    private int totalPenaltyPoints;

    // ======================
    // ATTENDANCE
    // ======================
    @Column(nullable = false)
    private String activityLevel;   // LOW / NORMAL / POSITIVE / OUTSTANDING

    @Column(nullable = false)
    private int attendanceBaseScore;

    @Column(nullable = false)
    private double attendanceMultiplier;

    @Column(nullable = false)
    private int attendanceTotalScore;

    // ======================
    // STAFF
    // ======================
    @Column(nullable = false)
    private int staffBaseScore;

    @Column(nullable = false)
    private int totalStaffCount;

    @Column(nullable = false)
    private String staffEvaluation;   // GOOD / AVERAGE / POOR

    @Column(nullable = false)
    private double staffMultiplier;

    @Column(nullable = false)
    private int staffScore;

    @Column(nullable = false)
    private int staffTotalScore;

    // ======================
    // CLUB
    // ======================
    @Column(nullable = false)
    private int totalClubSessions;

    @Column(nullable = false)
    private int totalClubPresent;

    @Column(nullable = false)
    private double sessionAttendanceRate;

    // ======================
    // FINAL
    // ======================
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
