package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QRToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qrTokenId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false, unique = true)
    private String tokenValue;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}

