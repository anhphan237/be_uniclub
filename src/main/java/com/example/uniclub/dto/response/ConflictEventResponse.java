package com.example.uniclub.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConflictEventResponse(
        Long eventId,
        String eventName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String locationName
) {}

