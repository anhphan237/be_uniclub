package com.example.uniclub.dto.response;

import java.time.LocalDate;

public record EventRegistrationResponse(
        Long eventId,
        String eventName,
        LocalDate date,
        String status,
        String hostClub
) {}
