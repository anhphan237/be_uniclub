package com.example.uniclub.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@RequiredArgsConstructor
public class LocationLogResponse {
    private Long eventId;
    private String eventName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}
