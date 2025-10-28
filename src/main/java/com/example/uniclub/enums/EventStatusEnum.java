package com.example.uniclub.enums;

public enum EventStatusEnum {
    WAITING_COCLUB_APPROVAL,   // Host gửi đơn - chờ co-club phản hồi
    WAITING_UNISTAFF_APPROVAL, // Tất cả co-club đồng ý - chờ UniStaff duyệt
    APPROVED,                  // Được duyệt - có thể đăng ký, điểm danh
    REJECTED,                  // Bị từ chối (bởi co-club hoặc UniStaff)
    CANCELLED,                 // CLB hoặc trường hủy
    FINISHED,                  // Sự kiện kết thúc, chờ settle
    SETTLED,                   // Hoàn tất điểm thưởng, hoàn ví
    COMPLETED                  // Giữ để tương thích cũ
}
