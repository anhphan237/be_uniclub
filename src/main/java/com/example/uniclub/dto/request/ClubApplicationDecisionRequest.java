package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClubApplicationDecisionRequest(
        @NotNull Boolean approve,
        String rejectReason
) {}
