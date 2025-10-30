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
    private List<Long> targetIds;   // danh sách clubId hoặc membershipId

    @Min(value = 1, message = "Số điểm phải lớn hơn 0.")
    private Long points;            // số điểm phát

    private String reason;          // lý do (tùy chọn)
}
