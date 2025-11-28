package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStaffStateEnum;

public record StaffInfoResponse(
        Long eventId,
        String eventName,
        Long clubId,
        String clubName,
        String duty,
        EventStaffStateEnum state
) {}
