package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum NotificationStatusEnum {
    UNREAD("UNREAD"),
    READ("READ");

    private final String code;

    NotificationStatusEnum(String code) {
        this.code = code;
    }
}

