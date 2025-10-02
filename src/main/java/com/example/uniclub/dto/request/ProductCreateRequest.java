package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotNull Long clubId,
        @NotBlank String name,
        String description,
        @NotNull @Positive Integer pricePoints,
        @NotNull @PositiveOrZero Integer stockQuantity
) {}
