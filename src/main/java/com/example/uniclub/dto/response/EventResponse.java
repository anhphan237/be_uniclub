package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;
    private EventStatusEnum status;
    private String checkInCode;

    private String locationName;
    private Integer maxCheckInCount;
    private Integer currentCheckInCount;

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
