package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private EventType type = EventType.PUBLIC;

    private LocalDate date;
    private LocalTime time;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
}
