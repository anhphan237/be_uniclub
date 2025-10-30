package com.example.uniclub.entity;

import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.RegistrationStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long registrationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private RegistrationStatusEnum status;

    private Integer committedPoints;
    private LocalDateTime registeredAt;
    private LocalDateTime checkinAt;
    private LocalDateTime canceledAt;
    @Column
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private AttendanceLevelEnum attendanceLevel;
    // ==== Attendance timestamps (3-phase) ====
    private java.time.LocalDateTime checkMidAt;   // giữa buổi
    private java.time.LocalDateTime checkoutAt;   // cuối buổi

    // ==== Fraud detection ====
    @Column(nullable = false)
    private boolean suspicious = false;

    @Column(length = 255)
    private String fraudReason;

}
