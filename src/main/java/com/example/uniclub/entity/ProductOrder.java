package com.example.uniclub.entity;

import com.example.uniclub.enums.OrderStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(
        name = "product_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_orders_order_code", columnNames = {"order_code"})
        },
        indexes = {
                @Index(name = "idx_product_orders_club", columnList = "club_id"),
                @Index(name = "idx_product_orders_membership", columnList = "membership_id"),
                @Index(name = "idx_product_orders_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    // 🔗 Sản phẩm được đổi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 🔗 Thành viên thực hiện đổi quà
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    // 🔗 CLB sở hữu sản phẩm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // 🔢 Số lượng hàng đổi
    @Column(nullable = false)
    private Integer quantity;

    // 💰 Tổng điểm trừ = pointCost * quantity
    @Column(nullable = false)
    private Long totalPoints;

    // ⚙️ Trạng thái đơn
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatusEnum status;

    // ⏰ Thời điểm tạo đơn
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ⏰ Thời điểm hoàn tất / hoàn trả
    private LocalDateTime completedAt;

    // 🆕 Mã đơn hàng ngắn (UC-Oxxxxxx)
    @Column(name = "order_code", length = 20, unique = true, nullable = false)
    private String orderCode;

    // 🆕 Ảnh QR encode Base64 (gửi kèm email)
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    // 🔁 Sinh mã đơn UC-Oxxxxxx tự động
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();

        if (this.orderCode == null || this.orderCode.isBlank()) {
            SecureRandom rnd = new SecureRandom();
            int n = 100000 + rnd.nextInt(900000);
            this.orderCode = "UC-O" + n;
        }
    }
}
