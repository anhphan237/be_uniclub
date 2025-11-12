package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRedeemOrderResponse {

    private Long id;               // ID của đơn hàng
    private String productName;    // Tên sản phẩm
    private String buyerName;      // Người đổi quà
    private String clubName;       // Tên CLB sở hữu sản phẩm
    private int quantity;          // Số lượng
    private long totalPoints;      // Tổng điểm trừ
    private String status;         // Trạng thái đơn (PENDING, COMPLETED, ...)
    private LocalDateTime createdAt;     // Thời điểm tạo
    private LocalDateTime completedAt;   // ✅ thêm để mapping không lỗi
    private String orderCode;      // Mã đơn hàng UC-Oxxxxx
    private String qrCodeBase64;   // Mã QR Base64
}
