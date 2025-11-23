package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ViolationLevelEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Negative;

public record PenaltyRuleRequest(

        @NotBlank
        String name,

        String description,

        @NotNull
        ViolationLevelEnum level,

        @NotNull
        @Negative
        Integer penaltyPoints

) {}
