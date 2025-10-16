package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClubApplicationCreateRequest(
        @NotBlank String clubName,
        String description,
        String category,
        @NotBlank String proposerReason
) {}
