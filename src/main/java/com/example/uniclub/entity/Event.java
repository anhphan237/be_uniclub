package com.example.uniclub.entity;

import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_club_id")
    private Club hostClub;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventCoClub> coHostRelations;

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

    @Column(nullable = false, unique = true, length = 50)
    private String checkInCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatusEnum status = EventStatusEnum.PENDING;

    @Column(nullable = false)
    private Integer currentCheckInCount = 0;

    private Integer maxCheckInCount;

    /** 🪙 Điểm cam kết mỗi thành viên khi tham gia */
    @Column(nullable = false)
    private Integer commitPointCost;

    /** 🔺 Trần hệ số thưởng (mặc định x2) */
    @Column(nullable = false)
    private Integer rewardMultiplierCap = 2;

    /** 💰 Ngân sách điểm do UniStaff cấp khi approve */
    @Column(nullable = false)
    private Integer budgetPoints = 0;

    /** 💼 Ví tách biệt của Event (escrow) */
    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Transient
    public List<Club> getCoHostedClubs() {
        return coHostRelations == null ? List.of()
                : coHostRelations.stream().map(EventCoClub::getClub).toList();
    }

    /** ✅ Helper: event đã settle/chốt hay chưa */
    @Transient
    public boolean isSettled() {
        return this.status == EventStatusEnum.SETTLED || this.status == EventStatusEnum.COMPLETED;
    }
    @ManyToMany
    @JoinTable(name = "event_accepted_clubs",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "club_id"))
    private List<Club> acceptedClubs = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "event_rejected_clubs",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "club_id"))
    private List<Club> rejectedClubs = new ArrayList<>();

}
