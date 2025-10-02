package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer pricePoints;

    @Column(nullable = false)
    private Integer stockQuantity = 0;
}
