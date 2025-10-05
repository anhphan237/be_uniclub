package com.example.uniclub.entity;

import com.example.uniclub.enums.ProductTxStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productTxId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "redeem_id")
    private Redeem redeem;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductTxStatusEnum status = ProductTxStatusEnum.RESERVED;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
