package com.example.uniclub.entity;

import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOwnerTypeEnum ownerType;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonBackReference // ✅ ngắt vòng lặp user <-> wallet
    private User user;

    @OneToOne
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(nullable = false)
    private Integer balancePoints = 0;
}
