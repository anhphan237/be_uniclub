package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RedeemOrderRequest(

        @Schema(example = "101", description = "ID của sản phẩm muốn đổi")
        @NotNull Long productId,

        @Schema(example = "2", description = "Số lượng muốn đổi, tối thiểu 1")
        @NotNull @Min(1) Integer quantity,

        @Schema(example = "45", description = "Optional khi staff quét QR redeem cho member")
        Long membershipId
) {}
