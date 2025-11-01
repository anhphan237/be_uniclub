package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ProductTypeEnum;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @Min(0) Integer pointCost,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull ProductTypeEnum type,
        Long eventId,                 // Bắt buộc nếu type = EVENT_ITEM
        List<Long> tagIds             // Admin quản lý tag; CLB chỉ chọn
) {}
