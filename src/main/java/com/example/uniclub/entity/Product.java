package com.example.uniclub.entity;

import com.example.uniclub.enums.ProductTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer pointCost;        // số điểm cần để đổi 1 đơn vị

    @Column(nullable = false)
    private Integer stockQuantity;    // tồn kho

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductTypeEnum type;     // CLUB_ITEM / EVENT_ITEM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;                // chủ sở hữu kho

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;              // nullable: chỉ set khi EVENT_ITEM

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, mediaId ASC")
    private List<ProductMedia> mediaList = new ArrayList<>();

    public void addMedia(ProductMedia media) {
        mediaList.add(media);
        media.setProduct(this);
    }
}
