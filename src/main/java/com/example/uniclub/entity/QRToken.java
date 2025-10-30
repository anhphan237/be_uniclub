package com.example.uniclub.entity;

import com.example.uniclub.enums.QRPhase;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QRToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qrTokenId;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, unique = true)
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QRPhase phase;   // START / MID / END

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
