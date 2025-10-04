package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record ClubCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull Long majorPolicyId
) {}
