package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum AttendanceLevelEnum {
    NONE(0, "Not attended"),
    HALF(1, "Attended 50% (Check-in + Mid-check)"),
    FULL(2, "Attended 100% (Check-in + Mid-check + Check-out)"),
    SUSPICIOUS(-1, "Suspicious activity (Check-in + Check-out, missing Mid-check)");


    private final int factor;
    private final String description;

    AttendanceLevelEnum(int factor, String description) {
        this.factor = factor;
        this.description = description;
    }
}
