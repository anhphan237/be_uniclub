package com.example.uniclub.dto.response;

import com.example.uniclub.entity.StaffPerformance;
import com.example.uniclub.enums.PerformanceLevelEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StaffPerformanceResponse {

    private Long id;
    private Long eventStaffId;
    private Long membershipId;
    private Long eventId;
    private PerformanceLevelEnum performance;
    private String note;
    private LocalDateTime createdAt;

    public static StaffPerformanceResponse from(StaffPerformance sp) {
        return StaffPerformanceResponse.builder()
                .id(sp.getId())
                .eventStaffId(sp.getEventStaff().getId())
                .membershipId(sp.getMembership().getMembershipId())
                .eventId(sp.getEvent().getEventId())
                .performance(sp.getPerformance())
                .note(sp.getNote())
                .createdAt(sp.getCreatedAt())
                .build();
    }
}
