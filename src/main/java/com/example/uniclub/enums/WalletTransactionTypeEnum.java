package com.example.uniclub.enums;

public enum WalletTransactionTypeEnum {
    // 🧾 Các loại cũ (giữ nguyên để không phá logic hiện tại)
    ADD,
    REDUCE,
    TRANSFER,
    UNI_TO_CLUB,
    CLUB_TO_MEMBER,

    // 🎉 Các loại giao dịch mới cho hệ thống Event
    EVENT_BUDGET_GRANT,  // UniStaff cấp ngân sách điểm vào ví sự kiện
    COMMIT_LOCK,         // Member join → trừ điểm cam kết từ ví membership vào ví event
    REFUND_COMMIT,       // Hoàn cọc (attendance ≥ 50%) từ ví event về ví membership
    BONUS_REWARD,        // Thưởng thêm (attendance = 100%) từ ví event về ví membership
    RETURN_SURPLUS,
    REDEEM_PRODUCT,
    REFUND_PRODUCT,
    EVENT_REDEEM_PRODUCT,
    EVENT_REFUND_PRODUCT,
    // Giao dịch giữa User ↔ CLB
    CLUB_RECEIVE_REDEEM,
    CLUB_REFUND,
    ADMIN_ADJUST
}
