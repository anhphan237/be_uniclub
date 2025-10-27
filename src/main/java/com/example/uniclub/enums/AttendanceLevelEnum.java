package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum AttendanceLevelEnum {
    NONE(0, "Chưa tham dự"),
    HALF(1, "Tham dự 50%"),
    FULL(2, "Tham dự 100%");

    private final int factor;
    private final String description;

    AttendanceLevelEnum(int factor, String description) {
        this.factor = factor;
        this.description = description;
    }
}
