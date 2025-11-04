package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletRewardBatchRequest {

    @NotEmpty(message = "Danh sách ID mục tiêu không được để trống.")
    private List<Long> targetIds;   // Danh sách userId nhận thưởng

    @Min(value = 1, message = "Số điểm phải lớn hơn 0.")
    private long points;            // Số điểm thưởng

    private String reason;          // Ghi chú (tùy chọn)
}
