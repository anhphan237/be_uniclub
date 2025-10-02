package com.example.uniclub.entity;

public enum RedeemStatus {
    CREATED,    // vừa khởi tạo, chưa xử lý
    COMPLETED,  // đã trừ điểm và giao hàng/nhận quà xong
    CANCELED    // hủy (do user hoặc admin)
}
