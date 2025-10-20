package com.example.uniclub.dto.request;

import lombok.Builder;

@Builder
public record ClubApplicationDecisionRequest(
        boolean approve,
        String rejectReason
) {}
