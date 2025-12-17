package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class LocationDailyLogResponse {
    private LocalDate date;
    private List<LocationEventTimeResponse> events;
}
