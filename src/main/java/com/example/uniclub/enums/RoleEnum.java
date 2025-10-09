package com.example.uniclub.enums;


import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN("ADMIN"),                 // Nhân viên IT, bảo trì hệ thống
    UNIVERSITY_STAFF("UNIVERSITY_STAFF"), // Nhân viên trường, quản lý toàn bộ CLB, phê duyệt event/club
    CLUB_LEADER("CLUB_LEADER"),     // Trưởng CLB
    MEMBER("MEMBER"),               // Thành viên CLB
    STUDENT("STUDENT");             // Sinh viên chưa join CLB

    private final String code;

    RoleEnum(String code) {
        this.code = code;
    }
}
