package com.example.uniclub.dto.request;

import lombok.Builder;

@Builder
public record ClubApplicationDecisionRequest(
        boolean approve,          // true = duyệt, false = từ chối
        String rejectReason,      // lý do từ chối (nếu có)
        String internalNote,      // ghi chú nội bộ
        String viceLeaderEmail,   // email phó chủ nhiệm (nếu có)
        String viceLeaderFullName,
        String viceLeaderStudentCode // cần nếu tạo user mới
) {}
