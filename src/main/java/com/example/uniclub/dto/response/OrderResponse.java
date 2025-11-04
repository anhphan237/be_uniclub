package com.example.uniclub.dto.response;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String orderCode,
        String productName,
        Integer quantity,
        Long totalPoints,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String clubName,
        String memberName
) {}
