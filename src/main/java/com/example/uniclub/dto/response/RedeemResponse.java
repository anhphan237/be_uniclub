package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RedeemResponse {
    private Long redeemId;
    private String productName;
    private Integer quantity;
    private Long totalCostPoints;
    private String status;

    // 🔹 Bổ sung để đồng bộ logic Redeem flow
    private Long productId;
    private String productType; // CLUB_ITEM / EVENT_ITEM
    private Long clubId;
    private Long eventId;

    // 🔹 Thông tin người thực hiện
    private Long userId;
    private String userName;

    // 🔹 Thông tin hiển thị thêm
    private String orderCode;
    private String createdAt;
    private String updatedAt;
}