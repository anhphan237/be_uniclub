package com.example.uniclub.dto.request;

import com.example.uniclub.enums.PerformanceLevelEnum;
import jakarta.validation.constraints.NotNull;

public record StaffPerformanceRequest(
        @NotNull Long membershipId,
        @NotNull Long eventId,
        @NotNull PerformanceLevelEnum performance,
        String note
) {}
