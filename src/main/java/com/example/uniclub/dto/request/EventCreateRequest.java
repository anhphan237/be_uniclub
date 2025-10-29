package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

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

        @Schema(type = "string", example = "09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime startTime,

        @Schema(type = "string", example = "15:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
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
