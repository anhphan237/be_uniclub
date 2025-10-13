package com.example.uniclub.enums;


import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN("ADMIN"),
    UNIVERSITY_STAFF("UNIVERSITY_STAFF"),
    CLUB_LEADER("CLUB_LEADER"),
//    MEMBER("MEMBER"),
    STUDENT("STUDENT");

    private final String code;

    RoleEnum(String code) {
        this.code = code;
    }
}
