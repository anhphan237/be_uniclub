package com.example.uniclub.enums;

public enum EventStatusEnum {
    PENDING,     // chờ duyệt từ nhà trường
    APPROVED,    // đã được duyệt và có thể đăng ký (đã có event wallet + budget)
    REJECTED,    // bị từ chối bởi nhà trường
    CANCELLED,   // bị hủy (bởi CLB hoặc trường)
    FINISHED,    // sự kiện đã kết thúc (điểm danh xong, chờ settle)
    SETTLED,     // đã quyết toán xong (refund/bonus/return surplus, đóng ví)
    COMPLETED    // tương đương SETTLED (giữ để backward compatibility)
}
