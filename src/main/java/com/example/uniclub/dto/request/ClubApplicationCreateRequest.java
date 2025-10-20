package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClubApplicationCreateRequest(
        @NotBlank String clubName,
        String description,
        @NotBlank String major,
        String vision,
        @NotBlank String proposerReason
) {}
