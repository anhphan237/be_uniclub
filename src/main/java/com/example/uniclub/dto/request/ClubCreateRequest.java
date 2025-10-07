package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClubCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull Long majorPolicyId,
        @NotNull Long majorId
) {}
