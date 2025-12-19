package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "club_monthly_activities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "year", "month"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMonthlyActivity {

    // =========================================================================
    //  PRIMARY KEY
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    //  RELATION
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // =========================================================================
    //  TIME
    // =========================================================================
    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    // =========================================================================
    //  EVENT METRICS
    // =========================================================================

    /** Tổng số event CLB tổ chức trong tháng */
    @Column(nullable = false)
    private int totalEvents;

    /**
     * Tỉ lệ event thành công
     * = completedEvents / totalEvents
     * Giá trị: 0 → 1 (đã làm tròn ở service)
     */
    @Column(name = "event_success_rate", nullable = false)
    private double eventSuccessRate;

    /** Feedback trung bình từ người tham gia */
    @Column(nullable = false)
    private double avgFeedback;

    /**
     * Trung bình lượt check-in / event
     * = totalCheckins / totalEvents
     */
    @Column(name = "avg_checkin_rate", nullable = false)
    private double avgCheckinRate;

    // =========================================================================
    //  MEMBER METRICS
    // =========================================================================

    /** Điểm hoạt động trung bình của member (nếu có dùng sau này) */
    @Column(nullable = false)
    private double avgMemberActivityScore;

    /** Tổng điểm thưởng CLB nhận trong tháng */
    @Column(name = "reward_points", nullable = false)
    private Long rewardPoints = 0L;

    // =========================================================================
    //  STAFF METRICS
    // =========================================================================

    /** Điểm đánh giá từ UniStaff (nếu có) */
    @Column(nullable = false)
    private double staffPerformanceScore;

    // =========================================================================
    //  LOCK STATE (SAU KHI TÍNH XONG)
    // =========================================================================

    @Column(nullable = false)
    private boolean locked = false;

    private String lockedBy;

    private LocalDateTime lockedAt;

    // =========================================================================
    //  FINAL SCORE
    // =========================================================================

    /** Điểm cuối cùng của CLB trong tháng */
    @Column(nullable = false)
    private double finalScore;

    /** Điểm dùng để xếp hạng & tính thưởng */
    @Column(nullable = false)
    private double awardScore;

    /** BRONZE / SILVER / GOLD */
    @Column(nullable = false, length = 50)
    private String awardLevel;

    // =========================================================================
    //  APPROVAL (UNI STAFF DUYỆT THƯỞNG)
    // =========================================================================

    @Column(nullable = false)
    private boolean approved = false;

    private LocalDateTime approvedAt;

    private String approvedBy;

    // =========================================================================
    //  AUDIT
    // =========================================================================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
