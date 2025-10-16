package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record MembershipApplyRequest(
        @NotNull Long clubId,
        String note
) {}
