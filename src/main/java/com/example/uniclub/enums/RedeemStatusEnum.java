package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum RedeemStatusEnum {
    CREATED("CREATED", "Just created, not yet processed"),
    COMPLETED("COMPLETED", "Points deducted and reward delivered"),
    CANCELED("CANCELED", "Redeem canceled (by user or admin)");

    private final String code;
    private final String description;

    RedeemStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

