package com.example.uniclub.enums;

public enum EventStatusEnum {
    PENDING_COCLUB,     // ⏳ Host gửi → chờ co-club duyệt
    PENDING_UNISTAFF,   // 🕓 Co-club duyệt xong → chờ UniStaff duyệt
    APPROVED,           // ✅ Đã duyệt → cho phép đăng ký, điểm danh
    ONGOING,            // 🟢 Đang diễn ra (trong ngày event)
    COMPLETED,          // 🏁 Kết thúc và settle toàn bộ điểm thưởng
    REJECTED,           // ❌ Bị từ chối (từ co-club hoặc UniStaff)
    CANCELLED           // 🚫 CLB hoặc trường hủy
}
