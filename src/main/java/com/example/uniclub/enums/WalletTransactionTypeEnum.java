package com.example.uniclub.enums;

public enum WalletTransactionTypeEnum {

    // ===== CORE =====
    ADD,
    REDUCE,
    TRANSFER,

    // ===== UNIVERSITY / CLUB =====
    UNI_TO_CLUB,
    CLUB_TO_MEMBER,

    // ===== EVENT =====
    EVENT_BUDGET_GRANT,
    EVENT_REFUND_REMAINING,
    EVENT_BUDGET_FORFEIT,

    // ===== COMMIT / BONUS =====
    COMMIT_LOCK,
    REFUND_COMMIT,
    BONUS_REWARD,
    RETURN_SURPLUS,
    PUBLIC_EVENT_REWARD,

    // ===== PRODUCT =====
    REDEEM_PRODUCT,
    REFUND_PRODUCT,
    EVENT_REDEEM_PRODUCT,
    EVENT_REFUND_PRODUCT,
    PRODUCT_CREATION_COST,
    PRODUCT_IMPORT_COST,

    // ===== CLUB =====
    CLUB_RECEIVE_REDEEM,
    CLUB_REFUND,
    CLUB_REWARD_DISTRIBUTE,

    // ===== PENALTY =====
    MEMBER_PENALTY,
    CLUB_FROM_PENALTY,

    // ===== ADMIN =====
    ADMIN_ADJUST,

    // ===== CASHOUT (MỚI) =====
    CASHOUT_REQUEST,     // CLB gửi đơn xin rút điểm
    CASHOUT_APPROVED,    // UniStaff duyệt đơn rút điểm (trừ điểm)
    CASHOUT_REJECTED     // UniStaff từ chối đơn rút điểm
}
