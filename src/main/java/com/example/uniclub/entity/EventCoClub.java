package com.example.uniclub.entity;

import com.example.uniclub.enums.EventCoHostStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_co_clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCoClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCoHostStatusEnum status = EventCoHostStatusEnum.PENDING;

    @Column
    private LocalDateTime respondedAt;

    @Column(length = 255)
    private String note;
}
