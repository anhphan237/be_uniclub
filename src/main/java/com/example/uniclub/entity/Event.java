package com.example.uniclub.entity;

import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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

    @Column(nullable = false)
    private Integer commitPointCost;

    @Column(nullable = false)
    private Integer rewardMultiplierCap = 2;

    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Transient
    public List<Club> getCoHostedClubs() {
        return coHostRelations.stream()
                .map(EventCoClub::getClub)
                .toList();
    }
}
