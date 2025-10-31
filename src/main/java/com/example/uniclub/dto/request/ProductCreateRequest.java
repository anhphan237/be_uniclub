package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ProductTypeEnum;
import jakarta.validation.constraints.*;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Integer pointCost,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull ProductTypeEnum type,
        Long eventId, // nullable: chỉ set khi EVENT_ITEM
        List<Long> tagIds // 🏷️ danh sách tag gắn với sản phẩm
) {}
