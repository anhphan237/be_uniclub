package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventTypeEnum;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EventCreateRequest(
        @NotNull Long clubId,
        @NotBlank String name,
        String description,
        @NotNull EventTypeEnum type,
        @NotNull LocalDate date,
        @NotBlank String time,
        Long locationId,
        @Positive(message = "maxCheckInCount must > 0")
        Integer maxCheckInCount
) {}
