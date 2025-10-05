package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum NotificationTypeEnum {
    SYSTEM("SYSTEM"),
    EVENT("EVENT"),
    REDEEM("REDEEM"),
    MEMBERSHIP("MEMBERSHIP"),
    OTHER("OTHER");

    private final String code;

    NotificationTypeEnum(String code) {
        this.code = code;
    }
}
