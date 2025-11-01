package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(

        @Schema(example = "202", description = "ID đơn hàng cần hoàn điểm")
        @NotNull Long orderId,

        @Schema(example = "1", description = "Số lượng sản phẩm hoàn lại")
        @NotNull @Min(1) Integer quantityToRefund
) {}
