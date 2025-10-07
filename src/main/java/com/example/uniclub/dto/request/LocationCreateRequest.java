package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record LocationCreateRequest(
        @NotBlank String name,
        String address,
        @PositiveOrZero Integer capacity
) {}
