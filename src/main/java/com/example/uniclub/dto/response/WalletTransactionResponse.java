package com.example.uniclub.dto.response;

import com.example.uniclub.entity.WalletTransaction;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionResponse {

    private Long id;
    private String type;                // Loại giao dịch
    private Long amount;                // Số điểm thay đổi
    private String description;         // Ghi chú / lý do
    private LocalDateTime createdAt;    // Thời gian tạo

    private String senderName;          // Ví gửi (CLB / Uni / User)
    private String receiverName;        // Ví nhận (CLB / Member / User)

    public static WalletTransactionResponse from(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType() != null ? tx.getType().name() : null)
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .senderName(
                        tx.getWallet() != null
                                ? getWalletOwnerName(tx)
                                : "Unknown Sender"
                )
                .receiverName(getReceiverName(tx))
                .build();
    }

    // 🧩 Helper: Lấy tên chủ ví gửi
    private static String getWalletOwnerName(WalletTransaction tx) {
        if (tx.getWallet().getClub() != null)
            return tx.getWallet().getClub().getName();
        if (tx.getWallet().getUser() != null)
            return tx.getWallet().getUser().getFullName();
        if (tx.getWallet().getEvent() != null)
            return tx.getWallet().getEvent().getName();
        return "Unknown Wallet Owner";
    }

    // 🧩 Helper: Lấy tên người/CLB nhận
    private static String getReceiverName(WalletTransaction tx) {
        if (tx.getReceiverClub() != null)
            return tx.getReceiverClub().getName();
        if (tx.getReceiverMembership() != null)
            return tx.getReceiverMembership().getUser().getFullName();
        if (tx.getReceiverUser() != null)
            return tx.getReceiverUser().getFullName();
        return "Unknown Receiver";
    }
}
