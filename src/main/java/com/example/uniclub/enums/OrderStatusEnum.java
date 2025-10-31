package com.example.uniclub.enums;

public enum OrderStatusEnum {
    PENDING,       // đã trừ điểm & giữ hàng, chờ staff/leader xác nhận giao
    COMPLETED,     // đã giao hàng, hoàn tất
    REFUNDED,      // đã hoàn điểm do hàng lỗi/không đúng mô tả
    PARTIALLY_REFUNDED// dùng để trả lại 1 phần hàng
}
