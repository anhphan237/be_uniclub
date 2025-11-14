package com.example.uniclub.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record LocationUpdateRequest(
        String name,
        String address,
        @PositiveOrZero Integer capacity
) {}
