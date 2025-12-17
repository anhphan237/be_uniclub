package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "location_event_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationEventHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_day_id")
    private Long eventDayId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (validFrom == null) validFrom = now;
        if (createdAt == null) createdAt = now;
    }
}
