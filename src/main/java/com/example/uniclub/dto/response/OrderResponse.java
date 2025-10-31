package com.example.uniclub.dto.response;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String productName,
        Integer quantity,
        Integer totalPoints,
        String status,
        LocalDateTime createdAt
) {}
