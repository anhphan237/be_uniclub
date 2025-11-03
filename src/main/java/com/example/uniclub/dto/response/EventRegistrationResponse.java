package com.example.uniclub.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventRegistrationResponse(
        Long eventId,
        String eventName,
        LocalDate date,
        String status,
        String hostClub,
        LocalDateTime registeredAt
) {}
