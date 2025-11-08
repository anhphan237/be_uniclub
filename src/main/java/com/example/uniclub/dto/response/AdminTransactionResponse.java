package com.example.uniclub.dto.response;

import com.example.uniclub.enums.WalletTransactionTypeEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AdminTransactionResponse {
    private Long id;
    private String senderName;
    private String receiverName;
    private WalletTransactionTypeEnum type;
    private Long amount;
    private LocalDateTime createdAt;
    private String note;
}
