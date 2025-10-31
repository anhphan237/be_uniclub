package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMedia {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 600)
    private String url;

    @Column(nullable = false, length = 10)
    private String type;       // IMAGE | VIDEO (đơn giản hoá, hoặc bạn có thể dùng enum)

    @Column(nullable = false)
    private boolean isThumbnail = false;

    @Column(nullable = false)
    private int displayOrder = 0;
}
