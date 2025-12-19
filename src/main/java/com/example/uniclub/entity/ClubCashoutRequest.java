package com.example.uniclub.entity;

import com.example.uniclub.enums.CashoutStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_cashout_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubCashoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================== RELATIONS ==================
    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // ================== POINT & MONEY ==================
    @Column(nullable = false)
    private Long pointsRequested;

    @Column(nullable = false)
    private Long cashAmount;

    @Column(nullable = false)
    private Integer exchangeRate;

    // ================== STATUS ==================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashoutStatusEnum status;

    // ================== NOTE ==================
    @Column(columnDefinition = "TEXT")
    private String leaderNote;

    @Column(columnDefinition = "TEXT")
    private String staffNote;

    // ================== AUDIT ==================
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;

    @PrePersist
    public void prePersist() {
        requestedAt = LocalDateTime.now();
    }
}
