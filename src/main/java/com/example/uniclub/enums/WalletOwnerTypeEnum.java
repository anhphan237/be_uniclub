package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum WalletOwnerTypeEnum {

    // 🏫 Ví của CLB
    CLUB("CLUB", "Wallet owned by a club"),

    // 👥 Ví của Membership (User–Club)
    USER("USER", "Wallet owned by a user within a club"),

    // 🎉 Ví của sự kiện
    EVENT("EVENT", "Wallet owned by an event");

    private final String code;
    private final String description;

    WalletOwnerTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
