package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClubApplicationCreateRequest(
        @NotBlank String clubName,
        String description,
        @NotNull Long majorId,
        String vision,
        @NotBlank String proposerReason,
        @NotBlank String studentEmail
) {}
