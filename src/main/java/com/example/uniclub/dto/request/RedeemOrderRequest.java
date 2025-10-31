package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RedeemOrderRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        // bắt buộc khi staff redeem tại booth event (quét QR -> membershipId)
        Long membershipId
) {}
