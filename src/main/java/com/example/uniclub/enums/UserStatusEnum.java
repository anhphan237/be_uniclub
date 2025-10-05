package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE("ACTIVE", "User is active and allowed to use the system"),
    INACTIVE("INACTIVE", "User is inactive, may need activation"),
    BANNED("BANNED", "User is banned and cannot access the system");

    private final String code;
    private final String description;

    UserStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}

