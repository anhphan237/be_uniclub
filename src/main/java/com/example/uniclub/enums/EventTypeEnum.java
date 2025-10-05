package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum EventTypeEnum {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    SPECIAL("SPECIAL");

    private final String code;

    EventTypeEnum(String code) {
        this.code = code;
    }

}

