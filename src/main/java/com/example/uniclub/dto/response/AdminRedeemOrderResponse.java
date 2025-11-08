package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRedeemOrderResponse {
    private Long id;
    private String productName;
    private String buyerName;
    private int quantity;
    private long totalPoints;
    private String status;
    private LocalDateTime createdAt;
    private String orderCode;
    private String qrCodeBase64;
}
