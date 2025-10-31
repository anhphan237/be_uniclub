package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ProductTypeEnum;
import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Integer pointCost,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull ProductTypeEnum type,
        Long eventId // nullable: chỉ set khi EVENT_ITEM
) {}
