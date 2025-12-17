package com.example.uniclub.dto.request;

import com.example.uniclub.enums.EventTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
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

        @NotEmpty(message = "Event must have at least 1 day")
        List<@Valid EventDayRequest> days,

        @NotNull(message = "Location ID is required")
        Long locationId,

        // ✅ PUBLIC event: số người check-in tối đa (limit theo location)
        @Positive(message = "maxCheckInCount must be > 0")
        Integer maxCheckInCount,

        // ✅ SPECIAL / PRIVATE: điểm cam kết (PUBLIC = 0)
        @Schema(
                description = "Commit points required for PRIVATE/SPECIAL events. "
                        + "This field is ignored and automatically set to 0 for PUBLIC events."
        )
        @PositiveOrZero(message = "commitPointCost must be >= 0")
        Integer commitPointCost,


        // 🔥 Thêm field deadline cho SPECIAL/PRIVATE
        @Schema(description = "Registration deadline (only for SPECIAL/PRIVATE events)")
        LocalDate registrationDeadline
) {}
