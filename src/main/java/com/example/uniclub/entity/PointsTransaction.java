package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PointsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // Số điểm +/- (EARN/TOPUP là dương, REDEEM âm, REFUND dương)
    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsTxType type;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // Liên hệ nghiệp vụ (vd: checkinId / redeemId / topupId)
    private Long referenceId;
}
