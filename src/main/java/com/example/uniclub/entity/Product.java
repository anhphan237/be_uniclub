package com.example.uniclub.entity;

import com.example.uniclub.enums.ProductStatusEnum;
import com.example.uniclub.enums.ProductTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_products_club", columnList = "club_id"),
                @Index(name = "idx_products_event", columnList = "event_id"),
                @Index(name = "idx_products_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    // 🆔 Mã sản phẩm duy nhất
    @Column(length = 20, unique = true, nullable = false)
    private String productCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long pointCost;

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

    // 🔹 Trạng thái hoạt động
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatusEnum status = ProductStatusEnum.ACTIVE;

    // 🔹 Cờ hiển thị (true cho ACTIVE & INACTIVE, false cho ARCHIVED)
    @Column(nullable = false)
    private Boolean isActive = true;

    // 🕓 Ngày tạo sản phẩm
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 🔁 Tổng lượt redeem
    @Column(nullable = false)
    @Builder.Default
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

    // ======================================================
    // 🧩 Helper methods
    // ======================================================
    public void addMedia(ProductMedia media) {
        mediaList.add(media);
        media.setProduct(this);
    }

    public void increaseRedeemCount(int count) {
        this.redeemCount = (this.redeemCount == null ? 0 : this.redeemCount) + count;

        // 🔥 Auto tag HOT khi đạt mốc redeem cao
        if (this.redeemCount >= 50 && this.getProductTags() != null) {
            boolean hasHot = this.productTags.stream()
                    .anyMatch(pt -> pt.getTag().getName().equalsIgnoreCase("hot"));
            if (!hasHot) {
                try {
                    Tag hotTag = Tag.builder().tagId(9L).name("hot").build(); // tag_id=9 từ DB bạn
                    ProductTag pt = ProductTag.builder()
                            .product(this)
                            .tag(hotTag)
                            .build();
                    this.productTags.add(pt);
                    System.out.println("🔥 Auto-tagged [hot] for product: " + this.getName());
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to auto-tag HOT for product " + this.getName() + ": " + e.getMessage());
                }
            }
        }
    }


    public void decreaseRedeemCount(int count) {
        if (this.redeemCount == null) this.redeemCount = 0;
        this.redeemCount = Math.max(0, this.redeemCount - count);
    }

    // ======================================================
    // ⚙️ Entity Lifecycle
    // ======================================================
    @PrePersist
    public void prePersist() {
        if (this.productCode == null || this.productCode.isBlank()) {
            this.productCode = randomCode();
        }
        if (this.status == null) this.status = ProductStatusEnum.ACTIVE;
        // ✅ Đúng quy ước: chỉ ARCHIVED mới false
        this.isActive = this.status != ProductStatusEnum.ARCHIVED;
    }

    @PreUpdate
    public void preUpdate() {
        // ✅ Đồng bộ lại theo quy ước
        this.isActive = this.status != ProductStatusEnum.ARCHIVED;
    }

    private String randomCode() {
        SecureRandom rnd = new SecureRandom();
        int n = 100000 + rnd.nextInt(900000);
        return "UC-P" + n;
    }
}
