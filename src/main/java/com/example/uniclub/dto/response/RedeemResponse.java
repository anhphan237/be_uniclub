package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RedeemResponse {
    private Long redeemId;
    private String productName;
    private Integer quantity;
    private Integer totalCostPoints;
    private String status;
}