package com.example.uniclub.dto.request;

import com.example.uniclub.enums.StockAdjustmentReason;
import jakarta.validation.constraints.NotNull;

public record ProductStockUpdateRequest(
        @NotNull Integer delta,
        StockAdjustmentReason reason, // bắt buộc khi delta < 0
        String note
) {}
