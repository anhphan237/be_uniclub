package com.example.uniclub.enums;

public enum EventStatusEnum {
    PENDING,     // chờ duyệt từ nhà trường
    APPROVED,    // đã được duyệt và có thể đăng ký
    REJECTED,    // bị từ chối bởi nhà trường
    CANCELLED,   // bị hủy (bởi CLB hoặc trường)
    COMPLETED    // sự kiện đã kết thúc và hoàn tất quy trình điểm thưởng
}
