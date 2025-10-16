package com.example.uniclub.entity;

import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club; // Host club

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private EventTypeEnum type = EventTypeEnum.PUBLIC;

    private LocalDate date;

    @Column(nullable = false)
    private String time;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false, unique = true, length = 50)
    private String checkInCode;

    @Column(nullable = false)
    private Integer currentCheckInCount = 0;

    private Integer maxCheckInCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatusEnum status = EventStatusEnum.PENDING;

    // 🟢 ví riêng cho Event (được cấp điểm khi APPROVED)
    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // 🟢 số điểm cam kết mỗi người khi đăng ký
    @Column(nullable = false)
    private Integer commitPointCost = 100; // default

    // 🟢 đặt trần nhân thưởng (1..3)
    @Column(nullable = false)
    private Integer rewardMultiplierCap = 3; // x1/x2/x3
}
