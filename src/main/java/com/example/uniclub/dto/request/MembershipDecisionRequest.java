package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record MembershipDecisionRequest(
        @NotNull Boolean approve,
        String rejectReason
) {}
