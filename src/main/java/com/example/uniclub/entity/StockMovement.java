package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    // + nhập kho, - xuất kho
    @Column(nullable = false)
    private Integer quantityChange;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
