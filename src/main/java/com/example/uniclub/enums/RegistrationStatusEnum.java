package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum RegistrationStatusEnum {
    PENDING("PENDING", "Waiting for approval"),
    APPROVED("APPROVED", "Registration has been approved"),
    REJECTED("REJECTED", "Registration was rejected"),
    CANCELED("CANCELED", "Registration was canceled");

    private final String code;
    private final String description;

    RegistrationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

