package com.example.uniclub.enums;

public enum ClubEventActivityEnum {
    LOW(0.9),
    NORMAL(1.0),
    POSITIVE(1.2),
    OUTSTANDING(1.4);

    public final double multiplier;

    ClubEventActivityEnum(double m) { this.multiplier = m; }

    public static ClubEventActivityEnum classify(int completedEvents) {
        if (completedEvents <= 1) return LOW;
        if (completedEvents < 5) return NORMAL;
        if (completedEvents < 7) return POSITIVE;
        return OUTSTANDING;
    }
}

