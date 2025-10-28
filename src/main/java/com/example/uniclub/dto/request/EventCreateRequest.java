package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventCreateRequest(

        @NotNull(message = "Host club ID is required")
        Long hostClubId,

        List<Long> coHostClubIds,

        @NotBlank(message = "Event name is required")
        String name,

        String description,

        @NotNull(message = "Event type is required")
        EventTypeEnum type,

        @NotNull(message = "Event date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @NotNull(message = "Location ID is required")
        Long locationId,

        @Positive(message = "maxCheckInCount must be > 0")
        Integer maxCheckInCount,

        @PositiveOrZero(message = "commitPointCost must be >= 0")
        Integer commitPointCost,

        // 🆕 Leader nhập luôn ngân sách mong muốn khi tạo event
        @NotNull(message = "Budget point is required")
        @Min(value = 0, message = "Budget point must be non-negative")
        Integer budgetPoints
) {}
