package com.example.uniclub.dto.response;

import lombok.Builder;

@Builder
public record ClubOverviewResponse(

        Long clubId,
        String clubName,

        Double ratingEvent,      // trung bình rating (host + cohost)
        Long totalCheckin,       // tổng checkin (host + cohost)
        Double checkinRate,      // trung bình currentCheckin / maxCheckin
        Long totalMember,        // số member ACTIVE
        Long totalStaff,         // tổng lần phân công staff ACTIVE
        Long totalBudgetEvent,   // tổng UNI_TO_EVENT + EVENT_BUDGET_GRANT
        Long totalProductEvent,  // số lượng product EVENT_ITEM redeem
        Long totalDiscipline,    // bạn không có bảng discipline → = 0
        Double attendanceRate    // trung bình attendance (FULL=1, HALF=0.5, NONE=0)
) {}
