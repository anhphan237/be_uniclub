package com.example.uniclub.entity;

import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.RegistrationStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatusEnum status = RegistrationStatusEnum.PENDING;

    private Integer committedPoints;

    private LocalDateTime registeredAt;

    // ========== 3 PHASE ATTENDANCE ==========
    private LocalDateTime checkinAt;   // START
    private LocalDateTime checkMidAt;  // MID
    private LocalDateTime checkoutAt;  // END

    // ========== CANCEL ==========
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceLevelEnum attendanceLevel = AttendanceLevelEnum.NONE;

    // ========== FRAUD DETECTION ==========
    @Builder.Default
    @Column(nullable = false)
    private boolean suspicious = false;

    @Column(length = 255)
    private String fraudReason;
}
