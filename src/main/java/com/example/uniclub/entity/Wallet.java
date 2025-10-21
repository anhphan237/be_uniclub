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

    // 🧩 Loại ví: CLUB / USER / EVENT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOwnerTypeEnum ownerType;

    // 🎓 Ví của User (mỗi user có 1 ví)
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonBackReference
    private User user;

    // 🏫 Ví của Club (mỗi club chỉ có 1 ví)
    @OneToOne
    @JoinColumn(name = "club_id", unique = true)
    private Club club;

    // 💰 Số điểm hiện có
    @Column(nullable = false)
    private Integer balancePoints = 0;
}
