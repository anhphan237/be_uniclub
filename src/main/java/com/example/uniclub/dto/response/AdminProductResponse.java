package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductResponse {
    private Long id;
    private String productCode;
    private String name;
    private String clubName;
    private String type;
    private String status;
    private Integer stockQuantity;
    private Long pointCost;
    private Integer redeemCount;
    private LocalDateTime createdAt;
}
