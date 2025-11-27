package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RefundRequest(

        @Schema(example = "202", description = "ID đơn hàng cần hoàn điểm")
        @NotNull Long orderId,

        @Schema(example = "1", description = "Số lượng hoàn lại (chỉ dùng cho partial refund)")
        Integer quantityToRefund,

        @Schema(example = "Sản phẩm bị lỗi bao bì", description = "Lý do hoàn hàng / refund")
        @NotBlank String reason



) {}
