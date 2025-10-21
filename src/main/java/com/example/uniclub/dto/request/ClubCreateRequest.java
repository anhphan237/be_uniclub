package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ClubCreateRequest(
        @NotBlank(message = "Club name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Major ID is required")
        Long majorId,

        String vision
) {}
