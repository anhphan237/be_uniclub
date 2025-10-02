package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkins")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkinId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    private LocalDateTime timestamp = LocalDateTime.now();

    private Integer pointsAwarded = 0;

    @ManyToOne
    @JoinColumn(name = "qr_token_id")
    private QRToken qrToken;

    @ManyToOne
    @JoinColumn(name = "registration_id")
    private EventRegistration registration;
}
