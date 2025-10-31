package com.example.uniclub.entity;

import com.example.uniclub.enums.OrderStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_orders_order_code", columnNames = {"order_code"})
        })
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
    private Integer totalPoints;

    // ⚙️ Trạng thái đơn
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatusEnum status;

    // ⏰ Thời điểm tạo đơn
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ⏰ Thời điểm hoàn tất / hoàn trả
    private LocalDateTime completedAt;

    // 🆕 Mã đơn hàng ngắn (UC-xxxxxx)
    @Column(name = "order_code", length = 20, unique = true)
    private String orderCode;

    // 🆕 Ảnh QR encode Base64 (gửi kèm email)
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;
}
