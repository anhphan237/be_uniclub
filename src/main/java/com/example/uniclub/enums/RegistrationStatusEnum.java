package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum RegistrationStatusEnum {

    PENDING("PENDING", "User registered but not confirmed"),
    CONFIRMED("CONFIRMED", "User successfully registered"),
    CHECKED_IN("CHECKED_IN", "User has checked in"),
    REWARDED("REWARDED", "User received event reward"),
    NO_SHOW("NO_SHOW", "User registered but did not attend"),
    CANCELED("CANCELED", "Registration canceled by user or system");

    private final String code;
    private final String description;

    RegistrationStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

