package com.example.uniclub.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletRewardBatchRequest {
    private List<Long> targetIds;   // danh sách clubId hoặc membershipId
    private Integer points;         // số điểm phát
    private String reason;          // lý do
}
