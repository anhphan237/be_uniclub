package com.example.uniclub.enums;

public enum EventStaffStateEnum {
    ACTIVE,     // được gán và event chưa kết thúc
    EXPIRED,    // event kết thúc -> tự hết hiệu lực
    REMOVED     // leader/vice gỡ thủ công trước khi kết thúc
}
