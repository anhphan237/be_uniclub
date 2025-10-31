package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(
        @NotNull Long orderId,
        @NotNull @Min(1) Integer quantityToRefund
) {}
