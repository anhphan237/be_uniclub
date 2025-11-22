package com.example.uniclub.enums;

public enum ActivityMultiplierEnum {
    SESSION_ATTENDANCE(0.5),
    STAFF_EVALUATION(1.0);

    public final double value;
    ActivityMultiplierEnum(double v) { this.value = v; }
}

