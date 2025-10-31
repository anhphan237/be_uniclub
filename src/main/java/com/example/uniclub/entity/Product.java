package com.example.uniclub.entity;

import com.example.uniclub.enums.ProductTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer pointCost;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductTypeEnum type; // CLUB_ITEM / EVENT_ITEM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event; // Nullable nếu là CLUB_ITEM

    @Column(nullable = false)
    private Boolean isActive = true;

    // 🕓 Ngày tạo sản phẩm
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 🔁 Tổng lượt redeem
    @Column(nullable = false)
    private Integer redeemCount = 0;

    // 🖼️ Danh sách media
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, mediaId ASC")
    @Builder.Default
    private List<ProductMedia> mediaList = new ArrayList<>();

    // 🏷️ Danh sách tag
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductTag> productTags = new ArrayList<>();

    // 🧩 Thêm media vào product
    public void addMedia(ProductMedia media) {
        mediaList.add(media);
        media.setProduct(this);
    }

    // 🧩 Tăng lượt redeem
    public void increaseRedeemCount(int count) {
        this.redeemCount = (this.redeemCount == null ? 0 : this.redeemCount) + count;
    }
}
