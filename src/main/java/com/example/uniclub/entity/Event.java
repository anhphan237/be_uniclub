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
@NoArgsConstructor
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

    // 🧾 Thống kê
    @Column(nullable = false)
    private Integer currentCheckInCount = 0;

    private Integer maxCheckInCount;

    // 🪙 Điểm cam kết
    @Column(nullable = false)
    private Integer commitPointCost;

    // 🔺 Hệ số thưởng trần
    @Column(nullable = false)
    private Integer rewardMultiplierCap = 2;

    // 💰 Ngân sách điểm (UniStaff cấp sau khi duyệt)
    @Column(name = "budget_points", nullable = false)
    private Long budgetPoints = 0L;
    // ❌ Lý do bị từ chối (nếu sự kiện bị reject bởi UniStaff hoặc Co-Club)
    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    // 👤 Người duyệt (staff hoặc admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

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
