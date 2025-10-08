package com.example.uniclub.enums;


import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN("ADMIN"),
    CLUB_MANAGER("CLUB_MANAGER"),
    STUDENT("STUDENT"),
    UNIVERSITY_ADMIN("UNIVERSITY_ADMIN");

    private final String code;

    RoleEnum(String code) {
        this.code = code;
    }
}
