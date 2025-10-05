package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatusEnum {
    SUBMITTED("SUBMITTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String code;

    ApplicationStatusEnum(String code) {
        this.code = code;
    }

}
