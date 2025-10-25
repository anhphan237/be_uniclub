package com.example.uniclub.entity;

import com.example.uniclub.enums.RedeemStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "redeems", indexes = {
        @Index(name = "idx_redeem_member", columnList = "member_id"),
        @Index(name = "idx_redeem_event", columnList = "event_id"),
        @Index(name = "idx_redeem_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Redeem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long redeemId;

    @ManyToOne(optional = false) @JoinColumn(name = "member_id")
    private Membership member; // người đổi

    @ManyToOne @JoinColumn(name = "event_id")
    private Event event; // event diễn ra (optional, có thể đổi tại booth event)

    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne @JoinColumn(name = "staff_id")
    private Membership staff; // người phát quà (set khi deliver)

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer totalCostPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RedeemStatusEnum status = RedeemStatusEnum.PENDING;

    @Column(nullable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deliveredAt;
}
