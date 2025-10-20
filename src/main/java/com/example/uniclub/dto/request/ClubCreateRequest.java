package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ClubCreateRequest(

        @NotBlank(message = "Club name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description,


        @NotBlank(message = "Major name is required")
        String majorName,

        String vision
) {}
