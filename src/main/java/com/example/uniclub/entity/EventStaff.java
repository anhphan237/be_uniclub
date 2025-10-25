package com.example.uniclub.entity;

import com.example.uniclub.enums.EventStaffStateEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_staffs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membership_id"}),
        indexes = {
                @Index(name = "idx_event_staff_event", columnList = "event_id"),
                @Index(name = "idx_event_staff_membership", columnList = "membership_id"),
                @Index(name = "idx_event_staff_state", columnList = "state")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(optional = false) @JoinColumn(name = "membership_id")
    private Membership membership;

    @Column(length = 100)
    private String duty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStaffStateEnum state = EventStaffStateEnum.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    private LocalDateTime unassignedAt;
}
