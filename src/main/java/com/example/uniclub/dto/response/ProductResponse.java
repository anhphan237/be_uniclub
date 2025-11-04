package com.example.uniclub.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        String description,
        Long pointCost,
        Integer stockQuantity,
        String type,
        String status,
        Long clubId,
        String clubName,
        Long eventId,
        LocalDateTime createdAt,
        Integer redeemCount,
        List<ProductMediaResponse> media,
        List<String> tags
) {}
