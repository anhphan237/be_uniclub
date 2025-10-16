package com.example.uniclub.dto.request;

import com.example.uniclub.enums.AttendanceLevelEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record EventCheckinRequest(
        @NotBlank String checkInCode,
        @NotNull AttendanceLevelEnum level
) {}
