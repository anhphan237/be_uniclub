package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ProductTypeEnum;
import com.example.uniclub.enums.ProductStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductUpdateRequest(
        @Size(max = 150) String name,
        String description,
        @Min(0) Integer pointCost,
        @Min(0) Integer stockQuantity,
        ProductTypeEnum type,    // Không khuyến nghị đổi type, nhưng để mềm
        Long eventId,            // Nếu đổi sang EVENT_ITEM
        ProductStatusEnum status,
        List<Long> tagIds
) {}
