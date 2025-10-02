package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "redeems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Redeem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long redeemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RedeemStatus status = RedeemStatus.CREATED;

    @Column(nullable = false)
    private LocalDateTime redeemedAt = LocalDateTime.now();
}
