package com.example.uniclub.entity;

import com.example.uniclub.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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
    @JoinColumn(name = "club_id")
    private Club club;

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
}
