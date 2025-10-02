package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ProductResponse {
    private Long id;
    private Long clubId;
    private String name;
    private String description;
    private Integer pricePoints;
    private Integer stockQuantity;
}
