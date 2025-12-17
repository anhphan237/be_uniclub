package com.example.uniclub.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class LocationLogResponse {
    private Long eventId;
    private String eventName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public LocationLogResponse(Long eventId, String name,
                               LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.eventId = eventId;
        this.eventName = name;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
