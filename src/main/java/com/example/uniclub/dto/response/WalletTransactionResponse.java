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
    private String type;
    private Long amount;
    private String description;
    private LocalDateTime createdAt;
    private String signedAmount;

    private String senderName;
    private String receiverName;

    public static WalletTransactionResponse from(WalletTransaction tx) {
        String typeName = tx.getType() != null ? tx.getType().name() : null;
        String signedAmount = calculateSignedAmount(typeName, tx.getAmount());

        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .type(typeName)
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .signedAmount(signedAmount)

                // 🔥 ƯU TIÊN LẤY TÊN TỪ DB
                .senderName(resolveSenderName(tx))
                .receiverName(resolveReceiverName(tx))

                .build();
    }

    // ===========================================
    // 🔥 FIX 1: Ưu tiên lấy senderName từ DB
    // ===========================================
    private static String resolveSenderName(WalletTransaction tx) {
        if (tx.getSenderName() != null && !tx.getSenderName().isBlank())
            return tx.getSenderName();

        // Nếu DB không có, fallback theo wallet
        if (tx.getWallet() != null) {
            if (tx.getWallet().getClub() != null)
                return tx.getWallet().getClub().getName();
            if (tx.getWallet().getUser() != null)
                return tx.getWallet().getUser().getFullName();
            if (tx.getWallet().getEvent() != null)
                return tx.getWallet().getEvent().getName();
        }

        // Fallback cuối cùng
        return "Unknown Sender";
    }

    // ===========================================
    // 🔥 FIX 2: Luôn lấy receiverName từ DB nếu có
    // ===========================================
    private static String resolveReceiverName(WalletTransaction tx) {
        if (tx.getReceiverName() != null && !tx.getReceiverName().isBlank())
            return tx.getReceiverName();

        if (tx.getReceiverClub() != null)
            return tx.getReceiverClub().getName();

        if (tx.getReceiverMembership() != null)
            return tx.getReceiverMembership().getUser().getFullName();

        if (tx.getReceiverUser() != null)
            return tx.getReceiverUser().getFullName();

        return "Unknown Receiver";
    }

    // ===========================================
    // 🧮 Helper: Tính dấu + hoặc -
    // ===========================================
    private static String calculateSignedAmount(String type, Long amount) {
        if (type == null || amount == null)
            return String.valueOf(amount);

        switch (type) {
            // + Điểm
            case "ADD":
            case "UNI_TO_CLUB":
            case "CLUB_TO_MEMBER":
            case "EVENT_BUDGET_GRANT":
            case "REFUND_COMMIT":
            case "BONUS_REWARD":
            case "RETURN_SURPLUS":
            case "REFUND_PRODUCT":
            case "EVENT_REFUND_PRODUCT":
                return "+" + amount;

            // - Điểm
            case "REDUCE":
            case "TRANSFER":
            case "COMMIT_LOCK":
            case "REDEEM_PRODUCT":
            case "EVENT_REDEEM_PRODUCT":
                return "-" + amount;

            default:
                return String.valueOf(amount);
        }
    }
}
