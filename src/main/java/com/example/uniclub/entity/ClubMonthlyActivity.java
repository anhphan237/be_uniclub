package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_monthly_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "year", "month"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ClubMonthlyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    private int year;
    private int month;

    // EVENT METRICS
    private int totalEvents;
    private double avgFeedback;
    private double avgCheckinRate;

    // MEMBER METRICS
    private double avgMemberActivityScore;
    @Column(name = "reward_points")
    private Long rewardPoints = 0L;

    // STAFF METRICS
    private double staffPerformanceScore;
    @Column(nullable = false)
    private boolean locked = false;
    private String lockedBy;

    @Column
    private LocalDateTime lockedAt;

    // FINAL
    private double finalScore;
    @Column(nullable = false)
    private double awardScore;

    @Column(nullable = false, length = 50)
    private String awardLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
