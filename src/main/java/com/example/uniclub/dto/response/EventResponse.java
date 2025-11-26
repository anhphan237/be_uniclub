package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private EventTypeEnum type;

    // ✅ GLOBAL RANGE
    private LocalDate startDate;   // ngày đầu tiên (min trong days)
    private LocalDate endDate;     // ngày cuối (max trong days)

    // ✅ LIST NGÀY CHI TIẾT
    private List<EventDayResponse> days;

    private EventStatusEnum status;
    private String checkInCode;
    private Long budgetPoints;
    private String locationName;
    private Integer maxCheckInCount;
    private Integer currentCheckInCount;
    private Integer commitPointCost;

    private SimpleClub hostClub;
    private List<SimpleClub> coHostedClubs;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleClub {
        private Long id;
        private String name;
        private EventCoHostStatusEnum coHostStatus;
    }
}
