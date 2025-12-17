package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class LocationEventTimeResponse {
    private Long eventId;
    private String eventName;
    private LocalTime startTime;
    private LocalTime endTime;
}
