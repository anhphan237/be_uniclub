package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // ✅ Format lại thời gian hiển thị kiểu "09:00"
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(type = "string", example = "09:00", description = "Event start time in HH:mm format")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(type = "string", example = "15:00", description = "Event end time in HH:mm format")
    private LocalTime endTime;

    private EventStatusEnum status;
    private String checkInCode;
    private Integer budgetPoints;
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
