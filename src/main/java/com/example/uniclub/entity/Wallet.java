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

    // 🧩 Loại ví: CLUB / MEMBERSHIP / EVENT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOwnerTypeEnum ownerType;

    // 🏫 Ví của CLB (mỗi CLB có 1 ví riêng)
    @OneToOne
    @JoinColumn(name = "club_id", unique = true)
    @JsonBackReference
    private Club club;

    // 👥 Ví của từng Membership (mỗi user–CLB có 1 ví riêng)
    @OneToOne
    @JoinColumn(name = "membership_id", unique = true)
    @JsonBackReference
    private Membership membership;

    // 💰 Số điểm hiện có
    @Column(nullable = false)
    private Integer balancePoints = 0;
}
