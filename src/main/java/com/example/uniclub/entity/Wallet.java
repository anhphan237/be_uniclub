package com.example.uniclub.entity;

import com.example.uniclub.enums.WalletOwnerTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOwnerTypeEnum ownerType; // USER hoặc CLUB

    // Một trong hai field dưới phải khác null (enforce ở service)
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "club_id")
    private Club club;

    // Có thể dùng như "cache hiển thị nhanh"; số dư chuẩn nên = sum(points_transactions.amount)
    @Column(nullable = false)
    private Integer balancePoints = 0;
}
