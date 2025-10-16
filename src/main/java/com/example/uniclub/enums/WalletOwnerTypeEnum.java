package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum WalletOwnerTypeEnum {
    USER("USER", "Wallet owned by a user"),
    CLUB("CLUB", "Wallet owned by a club"),
    EVENT("EVENT", "Wallet owned by an event");

    private final String code;
    private final String description;

    WalletOwnerTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

