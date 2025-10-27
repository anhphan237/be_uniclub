package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionResponse {
    private Long id;
    private String type;          // Loại giao dịch (IN / OUT / TOPUP / REWARD ...)
    private Integer amount;       // Số điểm thay đổi (+ / -)
    private String description;   // Mô tả giao dịch
    private LocalDateTime createdAt; // Thời điểm tạo
}
