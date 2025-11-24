package com.example.uniclub.dto.response;

import com.example.uniclub.entity.ProductOrder;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemOrderMiniResponse {

    private Long orderId;
    private String productName;
    private int quantity;
    private long totalPoints;

    public static RedeemOrderMiniResponse from(ProductOrder order) {
        return RedeemOrderMiniResponse.builder()
                .orderId(order.getOrderId())
                .productName(order.getProduct().getName())
                .quantity(order.getQuantity())
                .totalPoints(order.getTotalPoints())
                .build();
    }
}
