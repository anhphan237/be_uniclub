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

    // Trạng thái hiển thị/hoạt động
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatusEnum status = ProductStatusEnum.ACTIVE;

    // Cờ legacy (vẫn giữ tương thích — map theo status)
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

    // 🧩 Thêm media vào product
    public void addMedia(ProductMedia media) {
        mediaList.add(media);
        media.setProduct(this);
    }

    // 🧩 Tăng lượt redeem
    public void increaseRedeemCount(int count) {
        this.redeemCount = (this.redeemCount == null ? 0 : this.redeemCount) + count;
    }

    // Generate productCode trước khi persist
    @PrePersist
    public void prePersist() {
        if (this.productCode == null || this.productCode.isBlank()) {
            this.productCode = randomCode();
        }
        // Đồng bộ isActive theo status
        if (this.status == null) this.status = ProductStatusEnum.ACTIVE;
        this.isActive = this.status == ProductStatusEnum.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        // Đồng bộ isActive theo status
        this.isActive = this.status == ProductStatusEnum.ACTIVE;
    }

    private String randomCode() {
        SecureRandom rnd = new SecureRandom();
        int n = 100000 + rnd.nextInt(900000);
        return "UC-P" + n;
    }

}
