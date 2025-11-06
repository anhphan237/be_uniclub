package com.example.uniclub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(

        @Schema(example = "202", description = "ID đơn hàng cần hoàn điểm")
        @NotNull Long orderId,

        @Schema(example = "1", description = "Số lượng sản phẩm hoàn lại (nếu hoàn toàn thì gửi = tổng quantity của order)")
        @NotNull @Min(1) Integer quantityToRefund,

        @Schema(example = "Sản phẩm bị lỗi bao bì", description = "Lý do hoàn hàng / refund")
        @NotBlank String reason
) {}
