package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum PointsTxTypeEnum {
    EARN("EARN", "Earn points (check-in, bonus)"),
    REDEEM("REDEEM", "Redeem points (exchange for rewards)"),
    REFUND("REFUND", "Refund points (returns)"),
    TOPUP("TOPUP", "Top up points (convert from money)");

    private final String code;
    private final String description;

    PointsTxTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

