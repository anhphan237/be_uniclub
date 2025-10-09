package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record MemberApplicationCreateRequest(
        @NotNull Long clubId,
        String reason
) {}
