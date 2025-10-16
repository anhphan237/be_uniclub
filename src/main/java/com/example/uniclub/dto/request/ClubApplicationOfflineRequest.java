package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClubApplicationOfflineRequest(
        @NotBlank String clubName,
        String description,
        String category,
        @NotBlank String managerFullName,
        @NotBlank String deputyFullName,
        @NotNull Long majorId,
        @NotNull Long majorPolicyId
) {}
