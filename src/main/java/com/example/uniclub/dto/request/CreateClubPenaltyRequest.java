package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateClubPenaltyRequest(
        @NotNull Long membershipId,
        Long eventId,
        @NotNull Long ruleId,
        String reason
) {}

