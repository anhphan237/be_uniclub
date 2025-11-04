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
    private String signedAmount;        // ✅ Hiển thị + hoặc -

    private String senderName;          // Ví gửi (CLB / Uni / User)
    private String receiverName;        // Ví nhận (CLB / Member / User)

    // ✅ Build Response từ entity WalletTransaction
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

    // 🧮 Helper: Tính dấu + hoặc - dựa trên loại giao dịch
    private static String calculateSignedAmount(String type, Long amount) {
        if (type == null || amount == null) return String.valueOf(amount);

        switch (type) {
            // 🟢 Các loại cộng điểm
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

            // 🔴 Các loại trừ điểm
            case "REDUCE":
            case "TRANSFER":
            case "COMMIT_LOCK":
            case "REDEEM_PRODUCT":
            case "EVENT_REDEEM_PRODUCT":
                return "-" + amount;

            // ⚪ Mặc định
            default:
                return String.valueOf(amount);
        }
    }
}
