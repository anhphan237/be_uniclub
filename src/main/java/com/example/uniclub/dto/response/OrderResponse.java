package com.example.uniclub.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderCode,
        String productName,
        Integer quantity,
        Long totalPoints,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,

        String productType,   // CLUB_ITEM / EVENT_ITEM
        Long clubId,
        Long eventId,

        String clubName,
        String memberName,
        String reasonRefund,

        List<String> errorImages
) {}
