package com.example.uniclub.dto.request;

import com.example.uniclub.entity.EventType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventCreateRequest(
        @NotNull Long clubId,
        @NotBlank String name,
        String description,
        @NotNull EventType type,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        Long locationId
) {}
