
package com.example.uniclub.enums;

public enum PerformanceLevelEnum {
    POOR,       // kém
    AVERAGE,    // trung bình
    GOOD,       // tốt
    EXCELLENT;  // xuất sắc

    public boolean isExcellent() { return this == EXCELLENT; }
    public boolean isGood() { return this == GOOD; }
    public boolean isAverage() { return this == AVERAGE; }
    public boolean isPoor() { return this == POOR; }
}
