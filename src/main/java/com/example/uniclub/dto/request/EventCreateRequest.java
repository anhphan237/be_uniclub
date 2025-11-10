package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
        EventTypeEnum type, // ✅ PUBLIC / SPECIAL / PRIVATE

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

        // ✅ PUBLIC event: số người check-in tối đa (limit theo location)
        @Positive(message = "maxCheckInCount must be > 0")
        Integer maxCheckInCount,

        // ✅ SPECIAL / PRIVATE: điểm cam kết (PUBLIC = 0)
        @PositiveOrZero(message = "commitPointCost must be >= 0")
        Integer commitPointCost,

        // 🔥 Thêm field deadline cho SPECIAL/PRIVATE
        @Schema(description = "Registration deadline (only for SPECIAL/PRIVATE events)")
        LocalDate registrationDeadline
) {}
