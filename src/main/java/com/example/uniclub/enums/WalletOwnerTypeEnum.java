package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum WalletOwnerTypeEnum {


    CLUB("CLUB", "Wallet owned by a club"),

    USER("USER", "Wallet owned by a user within a club"),

    EVENT("EVENT", "Wallet owned by an event"),

    UNIVERSITY("UNIVERSITY", "Wallet owned by an university"),;

    private final String code;
    private final String description;

    WalletOwnerTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
