package com.example.uniclub.enums;

import lombok.Getter;

@Getter
public enum AttendanceLevelEnum {
    X1(1),
    X2(2),
    X3(3);

    private final int factor;

    AttendanceLevelEnum(int factor) {
        this.factor = factor;
    }
}
