package com.example.uniclub.entity;

import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    // 🎯 CLB chủ trì
    @ManyToOne(optional = false)
    @JoinColumn(name = "host_club_id")
    private Club hostClub;

    // 🤝 Danh sách co-host (quan hệ n-n mở rộng)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventCoClub> coHostRelations;

    // 📝 Thông tin chung
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private EventTypeEnum type = EventTypeEnum.PUBLIC;

    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    // 🔑 Mã check-in
    @Column(nullable = false, unique = true, length = 50)
    private String checkInCode;

    // 🧩 Trạng thái sự kiện
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatusEnum status = EventStatusEnum.PENDING_COCLUB;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;


    @Builder.Default
    @Column(nullable = false)
    private Integer currentCheckInCount = 0;


    @Column(name = "max_checkin_count")
    private Integer maxCheckInCount; // = sức chứa tối đa của sự kiện (theo location)


    // 🪙 Điểm cam kết
    @Builder.Default
    @Column(nullable = false)
    private Integer commitPointCost = 0;

    // 🔺 Hệ số thưởng trần
    @Builder.Default
    @Column(nullable = false)
    private Integer rewardMultiplierCap = 2;

    // 💰 Ngân sách điểm (UniStaff cấp sau khi duyệt)
    @Builder.Default
    @Column(name = "budget_points", nullable = false)
    private Long budgetPoints = 0L;
    // ❌ Lý do bị từ chối (nếu sự kiện bị reject bởi UniStaff hoặc Co-Club)
    @Column(columnDefinition = "TEXT")
    private String rejectReason;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;

    // 👤 Người duyệt (staff hoặc admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;


    // 💼 Ví của sự kiện
    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // =====================================================
    // 🧩 Helper Methods
    // =====================================================

    /** Lấy danh sách CLB đồng tổ chức */
    @Transient
    public List<Club> getCoHostedClubs() {
        return coHostRelations == null ? List.of()
                : coHostRelations.stream()
                .map(EventCoClub::getClub)
                .toList();
    }

    /** Kiểm tra event đã kết thúc hay chưa */
    @Transient
    public boolean isCompleted() {
        return this.status == EventStatusEnum.COMPLETED;
    }

    /** Kiểm tra event đang diễn ra không */
    @Transient
    public boolean isOngoing() {
        return this.status == EventStatusEnum.ONGOING;
    }
}
